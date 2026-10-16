package pizza.dlc.api.render.msdf;

public final class PizzaDLCFonts {
   private static MsdfFont regular;
   private static MsdfFont sfpro;
   private static MsdfFont nexaBook;
   private static MsdfFont nexaRegular;
   private static MsdfFont icomoon;

   private PizzaDLCFonts() {
   }

   public static MsdfFont regular() {
      if (regular == null) {
         regular = MsdfFont.load("regular");
      }

      return regular;
   }

   public static MsdfFont sfpro() {
      if (sfpro == null) {
         sfpro = MsdfFont.load("sfpro");
      }

      return sfpro;
   }

   public static MsdfFont nexaBook() {
      if (nexaBook == null) {
         nexaBook = MsdfFont.load("nexa_book");
      }
      return nexaBook;
   }

   public static MsdfFont nexaRegular() {
      if (nexaRegular == null) {
         nexaRegular = MsdfFont.load("nexa_regular");
      }
      return nexaRegular;
   }

   public static MsdfFont icomoon() {
      if (icomoon == null) {
         icomoon = MsdfFont.load("icomoon");
      }
      return icomoon;
   }
}
