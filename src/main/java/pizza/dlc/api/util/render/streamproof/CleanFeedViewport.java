package pizza.dlc.api.util.render.streamproof;

public record CleanFeedViewport(int x, int y, int width, int height) {
   public static CleanFeedViewport fit(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
      double scale = Math.min((double)targetWidth / sourceWidth, (double)targetHeight / sourceHeight);
      int width = Math.max(1, (int)(sourceWidth * scale));
      int height = Math.max(1, (int)(sourceHeight * scale));
      return new CleanFeedViewport((targetWidth - width) / 2, (targetHeight - height) / 2, width, height);
   }

   public double sourceX(double targetX, double sourceWidth) { return (targetX - this.x) * sourceWidth / this.width; }
   public double sourceY(double targetY, double sourceHeight) { return (targetY - this.y) * sourceHeight / this.height; }
   public double targetX(double sourceX, double sourceWidth) { return this.x + sourceX * this.width / sourceWidth; }
   public double targetY(double sourceY, double sourceHeight) { return this.y + sourceY * this.height / sourceHeight; }
}
