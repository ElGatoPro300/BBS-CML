package mchorse.bbs_mod.ui.dashboard.utils;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;

public class UIGraphPanel extends UIDashboardPanel
{
    public UIGraphCanvas canvas;
    public UITextbox expression;
    public UIIcon help;

    public UIGraphPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.canvas = new UIGraphCanvas();
        this.expression = new UITextbox(10000, this.canvas::parseExpression);
        this.help = new UIIcon(Icons.HELP, (b) -> UIUtils.openWebLink("https://github.com/mchorse/aperture/wiki/Math-Expressions"));
        this.help.tooltip(UIKeys.GRAPH_HELP, Direction.TOP);

        String first = "sin(x)";

        this.expression.setText(first);
        this.canvas.parseExpression(first);

        this.expression.relative(this).x(10).y(1F, -30).w(1F, -20).h(20);
        this.canvas.full(this);
        this.help.relative(this.expression).x(1F, -19).y(1).wh(18, 18);

        this.expression.add(this.help);
        this.add(this.canvas, this.expression);
    }
}
