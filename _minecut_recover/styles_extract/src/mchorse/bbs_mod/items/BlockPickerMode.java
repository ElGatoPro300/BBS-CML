package mchorse.bbs_mod.items;

public enum BlockPickerMode
{
    MODEL_BLOCK(0),
    IMPORT_TO_FILM(1);

    public final int index;

    BlockPickerMode(int index)
    {
        this.index = index;
    }

    public static BlockPickerMode fromIndex(int index)
    {
        if (index == IMPORT_TO_FILM.index)
        {
            return IMPORT_TO_FILM;
        }

        return MODEL_BLOCK;
    }
}
