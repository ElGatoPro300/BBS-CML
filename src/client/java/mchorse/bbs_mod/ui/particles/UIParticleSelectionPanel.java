package mchorse.bbs_mod.ui.particles;

import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.ui.dashboard.panels.UISelectionScreen;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIParticleSelectionPanel extends UISelectionScreen<ParticleScheme>
{
    public UIParticleSelectionPanel(UIParticleSchemePanel panel)
    {
        super(panel);
    }

    @Override
    protected Icon getFileIcon()
    {
        return Icons.PARTICLE;
    }
}
