package mchorse.bbs_mod.ui.supporters;

import mchorse.bbs_mod.resources.Link;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Supporters
{
    private List<Supporter> supporters = new ArrayList<>();

    public void setup()
    {
        /* Developers of BBS CML EDITION */
        this.add("Discord", "https://discord.gg/MAHVQBSce6", "textures/banners/CML.png").withDate(12, 1);
        this.add("ElGatoPro300", "https://www.youtube.com/@ElGatoPro300", "textures/banners/ElGatoPro300.png").withDate(12, 1);
        this.add("seb024xd", "https://www.youtube.com/@seb024yt", "textures/banners/seb024xd.png").withDate(12, 1);
        this.add("Diobede", "https://www.youtube.com/watch?v=iik25wqIuFo", "textures/banners/Diobede.png").withDate(12, 1);
        this.add("Fanyel", "http://youtube.com/@imLOSTStudios", "textures/banners/Fanyel.png").withDate(12, 1);
        this.add("Mattux", "https://www.youtube.com/@Mattux", "textures/banners/Mattux.png").withDate(12, 1);

        /* Founders of BBS CML EDITION */
        this.add("SEKZA_MC", "https://www.youtube.com/@secxavier49_official", "textures/banners/SEKZA_MC.png").withDate(12, 1);
        this.add("MrJack", "https://x.com/MrJackDnZ", "textures/banners/MrJack.png").withDate(12, 1);
        this.add("AymaWolf Omega", "https://www.youtube.com/@AymaWolfOmega", "textures/banners/AymaWolfOmega.png").withDate(12, 1);
        this.add("TobbyMC", "https://www.youtube.com/@TobbyMC", "textures/banners/TobbyMC.png").withDate(12, 1);
        this.add("JaviCubito", "https://www.youtube.com/@JaviCubito", "textures/banners/JaviCubito.png").withDate(12, 1);
        this.add("Jesuluto", "https://www.tiktok.com/@jesulutoxd", "textures/banners/Jesuluto.png").withDate(12, 1);
        this.add("Kazu_MC", "https://www.tiktok.com/@k4zuyuky", "textures/banners/Kazu_MC.png").withDate(12, 1);
        this.add("TheRocket", "https://www.youtube.com/@TheRocketttt", "textures/banners/TheRocket.png").withDate(12, 1);
        this.add("Yeyo Sin Contexto", "https://www.youtube.com/@YeyoSinContexto", "textures/banners/Yeyo Sin Contexto.png").withDate(12, 1);
        this.add("Redbirdpro", "https://www.youtube.com/@RedbirdproMC", "textures/banners/Redbirdpro.png").withDate(12, 1);
        this.add("MiniSunn_", "https://www.youtube.com/channel/UCoHzxz8f08OSs6LCD2QBr7Q", "textures/banners/MiniSunn_.png").withDate(12, 1);
        this.add("ItzUkyo2013", "https://www.x.com/@UkyoKounji_2013", "textures/banners/ItzUkyo2013.png").withDate(12, 1);
        this.add("SoyFrann", "https://www.youtube.com/channel/UCfkzCKdp23V6BD2iV7IEt9A", "textures/banners/SoyFrann.png").withDate(12, 1);
        this.add("Mokaccino", "https://www.youtube.com/@itzmokaccino", "textures/banners/Moka.png").withDate(12, 1);
        this.add("itzPizzaXD", "https://www.youtube.com/@ItzPizzaXD", "textures/banners/itzPizzaXD.png").withDate(12, 1);
        this.add("Pirata_12", "https://www.youtube.com/@Pirata_animations", "textures/banners/Pirata_12.png").withDate(12, 1);
        this.add("Soru San", "https://www.youtube.com/@Soru_San", "textures/banners/SoruSan.png").withDate(12, 1);
        this.add("SERGIO PLAYER", "https://www.youtube.com/c/SERGIOPLAYER15", "textures/banners/SERGIO_PLAYER.png").withDate(12, 1);
        this.add("Gooxy Series", "https://www.youtube.com/channel/UCVxJRlpLWvThRAqyj1zShyw", "textures/banners/GooxySeries.png").withDate(12, 1);
        this.add("ElRandi", "https://www.youtube.com/@ElRandiswe", "textures/banners/ElRandi.png").withDate(12, 1);

        /* Special Thanks */
        this.add("McHorse", "https://www.youtube.com/channel/UCSLuDXxxql4EVK_Ktd6PNbw", "textures/banners/mchorse.png").withDate(1, 1);
        this.add("Nioum", "https://www.youtube.com/@NioumMC", "textures/banners/Nioum.png").withDate(3, 27);
        this.add("FunkyFight", "https://www.youtube.com/@FunkyFight", "textures/banners/funkyfight.png").withDate(4, 2);
        this.add("Afegor (Alyokhin Dmitrii)", "https://www.youtube.com/@Afegor", "textures/banners/afegor.png").withDate(4, 8);
        this.add("SIRSYP", "https://www.youtube.com/@SIRSYP1", "textures/banners/SIRSYP.png").withDate(4, 8);
        this.add("Plixitizthz", "https://x.com/PlixtIz", "textures/banners/Plixitizthz.png").withDate(4, 8);
        this.add("bay4lly", "https://www.youtube.com/@bay4lly", "textures/banners/bay4lly.png").withDate(12, 1);
        this.add("AND_010", "https://www.youtube.com/@AND010", "textures/banners/AND_010.png").withDate(12, 1);
        this.add("SR400X", "https://x.com/SR400X_", "textures/banners/SR400X.png").withDate(12, 1);
    }

    private Supporter add(String name, String link, String banner)
    {
        return this.add(name, link, Link.create(banner));
    }

    private Supporter add(String name, String link, Link banner)
    {
        Supporter supporter = new Supporter(name, link, banner);

        this.supporters.add(supporter);

        return supporter;
    }

    public List<Supporter> getCCSupporters()
    {
        return this.supporters.stream().filter(Supporter::hasBanner).filter(s -> !s.name.equals("Discord") && !s.name.equals("ElGatoPro300") && !s.name.equals("seb024xd") && !s.name.equals("Diobede") && !s.name.equals("TobbyMC") && !s.name.equals("JaviCubito") && !s.name.equals("SEKZA_MC") && !s.name.equals("Jesuluto") && !s.name.equals("Kazu_MC") && !s.name.equals("SoyTon") && !s.name.equals("TheRocket") && !s.name.equals("AND_010") && !s.name.equals("Mattux") && !s.name.equals("SR400X") && !s.name.equals("MrJack") && !s.name.equals("Yeyo Sin Contexto") && !s.name.equals("Redbirdpro") && !s.name.equals("MiniSunn_") && !s.name.equals("lolinmalo") && !s.name.equals("ItzUkyo2013") && !s.name.equals("SoyFrann") && !s.name.equals("AymaWolf Omega") && !s.name.equals("Mokaccino") && !s.name.equals("itzPizzaXD") && !s.name.equals("Pirata_12") && !s.name.equals("Alabi Pictures") && !s.name.equals("McHorse") && !s.name.equals("Nioum") && !s.name.equals("FunkyFight") && !s.name.equals("Afegor (Alyokhin Dmitrii)") && !s.name.equals("Fanyel") && !s.name.equals("bay4lly") && !s.name.equals("SIRSYP") && !s.name.equals("Plixitizthz") && !s.name.equals("Soru San") && !s.name.equals("SERGIO PLAYER") && !s.name.equals("Gooxy Series") && !s.name.equals("ElRandi") ).sorted(Comparator.comparing((a) -> a.date)).collect(Collectors.toList());
    }

    public List<Supporter> getCMLSupporters()
    {
        return this.supporters.stream().filter(s -> s.name.equals("TobbyMC") || s.name.equals("JaviCubito") || s.name.equals("SEKZA_MC") || s.name.equals("Jesuluto") || s.name.equals("Kazu_MC") || s.name.equals("SoyTon") || s.name.equals("TheRocket") || s.name.equals("MrJack") || s.name.equals("Yeyo Sin Contexto") || s.name.equals("Redbirdpro") || s.name.equals("MiniSunn_") || s.name.equals("lolinmalo") || s.name.equals("ItzUkyo2013") || s.name.equals("SoyFrann") || s.name.equals("AymaWolf Omega") || s.name.equals("Mokaccino") || s.name.equals("itzPizzaXD") || s.name.equals("Pirata_12") || s.name.equals("Alabi Pictures") || s.name.equals("Soru San") || s.name.equals("SERGIO PLAYER") || s.name.equals("Gooxy Series") || s.name.equals("ElRandi") ).sorted(Comparator.comparing((a) -> a.date)).collect(Collectors.toList());
    }

    public List<Supporter> getCMLDevelopers()
    {
        return this.supporters.stream().filter(s -> s.name.equals("Discord") || s.name.equals("ElGatoPro300") || s.name.equals("seb024xd") || s.name.equals("Diobede") || s.name.equals("Fanyel") || s.name.equals("Mattux")) .sorted(Comparator.comparing((a) -> a.date)).collect(Collectors.toList());
    }

    public List<Supporter> getSpecialThanksSupporters()
    {
        return this.supporters.stream().filter(s -> s.name.equals("McHorse") || s.name.equals("Nioum") || s.name.equals("FunkyFight") || s.name.equals("Afegor (Alyokhin Dmitrii)") || s.name.equals("SIRSYP") || s.name.equals("Plixitizthz") || s.name.equals("bay4lly") || s.name.equals("SR400X") || s.name.equals("AND_010")).sorted(Comparator.comparing((a) -> a.date)).collect(Collectors.toList());
    }
}