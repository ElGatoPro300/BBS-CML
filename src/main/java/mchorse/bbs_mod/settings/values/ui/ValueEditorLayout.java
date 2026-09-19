package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class ValueEditorLayout extends BaseValue
{
    private EditorLayoutNode filmLayoutRoot = EditorLayoutNode.defaultFilmLayout();
    private List<EditorLayoutNode.SplitterNode> filmSplitters = new ArrayList<>();
    private float stateEditorSizeH = 0.7F;
    private float stateEditorSizeV = 0.25F;
    private int keyframeLabelWidth = 120;

    public ValueEditorLayout(String id)
    {
        super(id);
    }

    public EditorLayoutNode getFilmLayoutRoot()
    {
        return this.filmLayoutRoot;
    }

    public void setFilmLayoutRoot(EditorLayoutNode root)
    {
        BaseValue.edit(this, (v) ->
        {
            this.filmLayoutRoot = root;
            this.filmSplitters.clear();
            EditorLayoutNode.collectSplitters(root, this.filmSplitters);
        });
    }

    public List<EditorLayoutNode.SplitterNode> getFilmSplitters()
    {
        return this.filmSplitters;
    }

    public void setFilmSplitterRatio(int index, float ratio)
    {
        if (index < 0 || index >= this.filmSplitters.size())
        {
            return;
        }
        int i = index;
        BaseValue.edit(this, (v) -> this.filmSplitters.get(i).setRatio(MathUtils.clamp(ratio, 0.05F, 0.95F)));
    }

    public float getFilmMainRatio()
    {
        if (this.filmLayoutRoot instanceof EditorLayoutNode.SplitterNode)
        {
            return ((EditorLayoutNode.SplitterNode) this.filmLayoutRoot).getRatio();
        }
        return 0.66F;
    }

    public float getFilmSmallRatio()
    {
        if (this.filmLayoutRoot instanceof EditorLayoutNode.SplitterNode)
        {
            EditorLayoutNode second = ((EditorLayoutNode.SplitterNode) this.filmLayoutRoot).getSecond();
            if (second instanceof EditorLayoutNode.SplitterNode)
            {
                return ((EditorLayoutNode.SplitterNode) second).getRatio();
            }
        }
        return 0.5F;
    }

    public void setFilmRatios(float mainRatio, float smallRatio)
    {
        BaseValue.edit(this, (v) ->
        {
            if (this.filmLayoutRoot instanceof EditorLayoutNode.SplitterNode)
            {
                EditorLayoutNode.SplitterNode root = (EditorLayoutNode.SplitterNode) this.filmLayoutRoot;
                root.setRatio(MathUtils.clamp(mainRatio, 0.05F, 0.95F));
                EditorLayoutNode second = root.getSecond();
                if (second instanceof EditorLayoutNode.SplitterNode)
                {
                    ((EditorLayoutNode.SplitterNode) second).setRatio(MathUtils.clamp(smallRatio, 0.05F, 0.95F));
                }
            }
        });
    }

    public void setFilmMainRatio(float mainRatio)
    {
        BaseValue.edit(this, (v) ->
        {
            if (this.filmLayoutRoot instanceof EditorLayoutNode.SplitterNode)
            {
                ((EditorLayoutNode.SplitterNode) this.filmLayoutRoot).setRatio(MathUtils.clamp(mainRatio, 0.05F, 0.95F));
            }
        });
    }

    public void setFilmSmallRatio(float smallRatio)
    {
        BaseValue.edit(this, (v) ->
        {
            if (this.filmLayoutRoot instanceof EditorLayoutNode.SplitterNode)
            {
                EditorLayoutNode second = ((EditorLayoutNode.SplitterNode) this.filmLayoutRoot).getSecond();
                if (second instanceof EditorLayoutNode.SplitterNode)
                {
                    ((EditorLayoutNode.SplitterNode) second).setRatio(MathUtils.clamp(smallRatio, 0.05F, 0.95F));
                }
            }
        });
    }

    public void setStateEditorSizeH(float stateEditorSizeH)
    {
        BaseValue.edit(this, (v) -> this.stateEditorSizeH = stateEditorSizeH);
    }

    public void setStateEditorSizeV(float stateEditorSizeV)
    {
        BaseValue.edit(this, (v) -> this.stateEditorSizeV = stateEditorSizeV);
    }

    public float getStateEditorSizeH()
    {
        return MathUtils.clamp(this.stateEditorSizeH, 0.1F, 0.9F);
    }

    public float getStateEditorSizeV()
    {
        return MathUtils.clamp(this.stateEditorSizeV, 0.1F, 0.9F);
    }

    public int getKeyframeLabelWidth()
    {
        return MathUtils.clamp(this.keyframeLabelWidth, 40, 400);
    }

    public void setKeyframeLabelWidth(int keyframeLabelWidth)
    {
        BaseValue.edit(this, (v) -> this.keyframeLabelWidth = MathUtils.clamp(keyframeLabelWidth, 40, 400));
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();
        data.put("film_layout", this.filmLayoutRoot.toData());
        data.putFloat("state_editor_size_h", this.stateEditorSizeH);
        data.putFloat("state_editor_size_v", this.stateEditorSizeV);
        data.putInt("keyframe_label_width", this.keyframeLabelWidth);
        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isMap())
        {
            MapType map = data.asMap();

            if (map.has("film_layout"))
            {
                this.filmLayoutRoot = EditorLayoutNode.fromData(map.get("film_layout"));
                if (this.filmLayoutRoot == null)
                {
                    this.filmLayoutRoot = EditorLayoutNode.defaultFilmLayout();
                }
                this.filmSplitters.clear();
                EditorLayoutNode.collectSplitters(this.filmLayoutRoot, this.filmSplitters);
            }
            else
            {
                float mainV = map.getFloat("main_size_v", 0.66F);
                float editorV = map.getFloat("editor_size_v", 0.5F);
                this.filmLayoutRoot = EditorLayoutNode.defaultFilmLayout();
                if (this.filmLayoutRoot instanceof EditorLayoutNode.SplitterNode)
                {
                    EditorLayoutNode.SplitterNode root = (EditorLayoutNode.SplitterNode) this.filmLayoutRoot;
                    root.setRatio(MathUtils.clamp(mainV, 0.05F, 0.95F));
                    EditorLayoutNode second = root.getSecond();
                    if (second instanceof EditorLayoutNode.SplitterNode)
                    {
                        ((EditorLayoutNode.SplitterNode) second).setRatio(MathUtils.clamp(editorV, 0.05F, 0.95F));
                    }
                }
                this.filmSplitters.clear();
                EditorLayoutNode.collectSplitters(this.filmLayoutRoot, this.filmSplitters);
            }

            this.stateEditorSizeH = map.getFloat("state_editor_size_h", 0.7F);
            this.stateEditorSizeV = map.getFloat("state_editor_size_v", 0.25F);
            this.keyframeLabelWidth = map.getInt("keyframe_label_width", 120);
        }
    }
}
