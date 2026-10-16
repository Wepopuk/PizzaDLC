package pizza.dlc.api.account;

import pizza.dlc.api.translation.Translations;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public final class MicrosoftAuth {
   private static final String CLIENT_ID = "00000000402b5328";
   private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
   private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";
   private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
   private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
   private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
   private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
   private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
   private static final String[] RPS_PREFIXES = new String[]{"d=", "t=", ""};
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

   private MicrosoftAuth() {
   }

   public record DeviceCode(String userCode, String deviceCode, String verificationUri, int intervalSec, long expiresAtMs) {
   }

   public record MsaTokens(String accessToken, String refreshToken) {
   }

   public record Session(String name, UUID uuid, String accessToken, long expiresAtMs, String refreshToken) {
   }

   public static final class AuthException extends Exception {
      public AuthException(String message) {
         super(message);
      }
   }

   public static DeviceCode requestDeviceCode() throws AuthException {
      JsonObject r = postForm(DEVICE_CODE_URL, Map.of("client_id", CLIENT_ID, "scope", SCOPE, "response_type", "device_code"));
      if (!r.has("user_code") || !r.has("device_code")) {
         throw new AuthException(Translations.text("Microsoft не выдал код входа"));
      }

      int interval = r.has("interval") ? r.get("interval").getAsInt() : 5;
      long expires = System.currentTimeMillis() + (r.has("expires_in") ? r.get("expires_in").getAsLong() : 900L) * 1000L;
      String uri = r.has("verification_uri") ? r.get("verification_uri").getAsString() : "https://www.microsoft.com/link";
      return new DeviceCode(r.get("user_code").getAsString(), r.get("device_code").getAsString(), uri, interval, expires);
   }

   public static MsaTokens pollDeviceCode(DeviceCode code, BooleanSupplier cancelled) throws AuthException {
      int interval = Math.max(1, code.intervalSec());

      while (!cancelled.getAsBoolean()) {
         if (System.currentTimeMillis() > code.expiresAtMs()) {
            throw new AuthException(Translations.text("Код истёк — начни вход заново"));
         }

         sleep(interval * 1000L, cancelled);
         if (cancelled.getAsBoolean()) {
            break;
         }

         Map<String, String> form = new LinkedHashMap<>();
         form.put("client_id", CLIENT_ID);
         form.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
         form.put("device_code", code.deviceCode());
         JsonObject r = postFormRaw(TOKEN_URL, form);
         if (r.has("access_token")) {
            return tokensOf(r);
         }

         String error = r.has("error") ? r.get("error").getAsString() : "";
         switch (error) {
            case "authorization_pending":
               break;
            case "slow_down":
               interval += 5;
               break;
            case "expired_token":
               throw new AuthException(Translations.text("Код истёк — начни вход заново"));
            case "authorization_declined":
            case "access_denied":
               throw new AuthException(Translations.text("Вход отменён в браузере"));
            default:
               throw new AuthException("Microsoft: " + (error.isEmpty() ? Translations.text("неизвестная ошибка") : error));
         }
      }

      throw new AuthException(Translations.text("Отменено"));
   }

   public static MsaTokens refresh(String refreshToken) throws AuthException {
      Map<String, String> form = new LinkedHashMap<>();
      form.put("client_id", CLIENT_ID);
      form.put("scope", SCOPE);
      form.put("grant_type", "refresh_token");
      form.put("refresh_token", refreshToken);
      JsonObject r = postFormRaw(TOKEN_URL, form);
      if (!r.has("access_token")) {
         throw new AuthException(Translations.text("Сессия Microsoft истекла — войди заново"));
      }

      return tokensOf(r);
   }

   public static Session minecraftLogin(MsaTokens msa) throws AuthException {
      JsonObject xbl = xboxUserToken(msa.accessToken());
      String xblToken = xbl.get("Token").getAsString();
      JsonObject xstsBody = new JsonObject();
      JsonObject props = new JsonObject();
      props.addProperty("SandboxId", "RETAIL");
      JsonArray userTokens = new JsonArray();
      userTokens.add(xblToken);
      props.add("UserTokens", userTokens);
      xstsBody.add("Properties", props);
      xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
      xstsBody.addProperty("TokenType", "JWT");
      HttpResponse<String> xstsResp = postJson(XSTS_URL, xstsBody, null);
      if (xstsResp.statusCode() == 401) {
         throw new AuthException(xstsError(xstsResp.body()));
      }

      JsonObject xsts = parse(xstsResp, "Xbox (XSTS)");
      String xstsToken = xsts.get("Token").getAsString();
      String uhs = xsts.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
      JsonObject mcBody = new JsonObject();
      mcBody.addProperty("identityToken", "0 x=" + uhs + ";" + xstsToken);
      JsonObject mc = parse(postJson(MC_LOGIN_URL, mcBody, null), "Minecraft");
      String mcToken = mc.get("access_token").getAsString();
      long expires = System.currentTimeMillis() + (mc.has("expires_in") ? mc.get("expires_in").getAsLong() : 86400L) * 1000L;
      HttpResponse<String> profileResp = get(MC_PROFILE_URL, mcToken);
      if (profileResp.statusCode() == 404) {
         throw new AuthException(Translations.text("На этом аккаунте нет Minecraft: Java Edition"));
      }

      JsonObject profile = parse(profileResp, Translations.text("профиль Minecraft"));
      String name = profile.get("name").getAsString();
      UUID uuid = undashed(profile.get("id").getAsString());
      return new Session(name, uuid, mcToken, expires, msa.refreshToken());
   }

   private static JsonObject xboxUserToken(String msaAccessToken) throws AuthException {
      int lastStatus = 0;

      for (String prefix : RPS_PREFIXES) {
         JsonObject body = new JsonObject();
         JsonObject props = new JsonObject();
         props.addProperty("AuthMethod", "RPS");
         props.addProperty("SiteName", "user.auth.xboxlive.com");
         props.addProperty("RpsTicket", prefix + msaAccessToken);
         body.add("Properties", props);
         body.addProperty("RelyingParty", "http://auth.xboxlive.com");
         body.addProperty("TokenType", "JWT");
         HttpResponse<String> resp = postJson(XBL_URL, body, null);
         if (resp.statusCode() == 200) {
            return JsonParser.parseString(resp.body()).getAsJsonObject();
         }

         lastStatus = resp.statusCode();
      }

      throw new AuthException(Translations.text("Xbox Live отклонил вход (HTTP ") + lastStatus + ")");
   }

   private static String xstsError(String body) {
      try {
         long code = JsonParser.parseString(body).getAsJsonObject().get("XErr").getAsLong();
         if (code == 2148916233L) {
            return Translations.text("У аккаунта нет профиля Xbox — создай его на xbox.com");
         }

         if (code == 2148916235L) {
            return Translations.text("Xbox Live недоступен в твоей стране");
         }

         return code != 2148916236L && code != 2148916237L && code != 2148916238L
            ? Translations.text("Xbox отклонил вход (код ") + code + ")"
            : Translations.text("Детский аккаунт — нужно добавить его в семью Microsoft");
      } catch (Exception ignored) {
         return Translations.text("Xbox отклонил вход");
      }
   }

   private static MsaTokens tokensOf(JsonObject r) {
      return new MsaTokens(r.get("access_token").getAsString(), r.has("refresh_token") ? r.get("refresh_token").getAsString() : null);
   }

   private static UUID undashed(String id) {
      String d = id.length() == 32
         ? id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20)
         : id;
      return UUID.fromString(d);
   }

   private static void sleep(long ms, BooleanSupplier cancelled) {
      long end = System.currentTimeMillis() + ms;

      while (System.currentTimeMillis() < end && !cancelled.getAsBoolean()) {
         try {
            Thread.sleep(100L);
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return;
         }
      }
   }

   private static JsonObject postForm(String url, Map<String, String> form) throws AuthException {
      JsonObject r = postFormRaw(url, form);
      if (r.has("error")) {
         throw new AuthException("Microsoft: " + r.get("error").getAsString());
      }

      return r;
   }

   private static JsonObject postFormRaw(String url, Map<String, String> form) throws AuthException {
      String body = form.entrySet()
         .stream()
         .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
         .collect(Collectors.joining("&"));
      HttpRequest req = HttpRequest.newBuilder(URI.create(url))
         .timeout(Duration.ofSeconds(20))
         .header("Content-Type", "application/x-www-form-urlencoded")
         .header("Accept", "application/json")
         .POST(HttpRequest.BodyPublishers.ofString(body))
         .build();

      try {
         HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
         return JsonParser.parseString(resp.body()).getAsJsonObject();
      } catch (IOException | IllegalStateException e) {
         throw new AuthException(Translations.text("Нет связи с Microsoft"));
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new AuthException(Translations.text("Отменено"));
      }
   }

   private static HttpResponse<String> postJson(String url, JsonObject body, String bearer) throws AuthException {
      HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
         .timeout(Duration.ofSeconds(20))
         .header("Content-Type", "application/json")
         .header("Accept", "application/json")
         .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
      if (bearer != null) {
         b.header("Authorization", "Bearer " + bearer);
      }

      return send(b.build());
   }

   private static HttpResponse<String> get(String url, String bearer) throws AuthException {
      return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + bearer).GET().build());
   }

   private static HttpResponse<String> send(HttpRequest req) throws AuthException {
      try {
         return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
      } catch (IOException e) {
         throw new AuthException(Translations.text("Нет связи с сервером входа"));
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new AuthException(Translations.text("Отменено"));
      }
   }

   private static JsonObject parse(HttpResponse<String> resp, String what) throws AuthException {
      if (resp.statusCode() / 100 != 2) {
         throw new AuthException(Translations.text("Ошибка входа: ") + what + " (HTTP " + resp.statusCode() + ")");
      }

      try {
         return JsonParser.parseString(resp.body()).getAsJsonObject();
      } catch (Exception ignored) {
         throw new AuthException(Translations.text("Некорректный ответ: ") + what);
      }
   }
}
