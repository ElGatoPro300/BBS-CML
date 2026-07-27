package mchorse.bbs_mod.utils.watchdog;

import java.nio.file.Path;

public interface IWatchDogListener
{
    public void accept(Path path, WatchDogEvent event);
}