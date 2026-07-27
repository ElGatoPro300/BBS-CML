package mchorse.bbs_mod.ui.utils;

/**
 * Scroll direction
 */
public enum ScrollDirection
{
    VERTICAL()
    {
        @Override
        public int getPosition(Area area, float x)
        {
            return area.y(x);
        }

        @Override
        public int getSide(Area area)
        {
            return area.h;
        }

        @Override
        public int getScroll(Area area, Scroll scroll, int x, int y)
        {
            return y - area.y + (int) scroll.getScroll();
        }

        @Override
        public float getMouse(int x, int y)
        {
            return y;
        }
    },
    HORIZONTAL()
    {
        @Override
        public int getPosition(Area area, float x)
        {
            return area.x(x);
        }

        @Override
        public int getSide(Area area)
        {
            return area.w;
        }

        @Override
        public int getScroll(Area area, Scroll scroll, int x, int y)
        {
            return x - area.x + (int) scroll.getScroll();
        }

        @Override
        public float getMouse(int x, int y)
        {
            return x;
        }
    };

    /**
     * Get position of the area, x = 0 minimum corner, x = 1 maximum corner
     */
    public abstract int getPosition(Area area,  float x);

    /**
     * Get dominant side for this scrolling direction
     */
    public abstract int getSide(Area area);

    /**
     * Get scrolled amount for given mouse position
     */
    public abstract int getScroll(Area area, Scroll scroll, int x, int y);

    public abstract float getMouse(int x, int y);
}