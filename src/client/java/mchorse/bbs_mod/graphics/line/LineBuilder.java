package mchorse.bbs_mod.graphics.line;

import mchorse.bbs_mod.graphics.GuiQuadMesh;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

import org.joml.Matrix3x2fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Line builder 2D
 *
 * This class provides a neat way to construct 2D line
 * segments that is thicker than default OpenGL3 line renderer.
 */
public class LineBuilder <T>
{
    public float thickness;
    public List<Line<T>> lines = new ArrayList<>();

    public LineBuilder(float thickness)
    {
        this.thickness = thickness;
    }

    public LineBuilder<T> add(float x, float y)
    {
        return this.add(x, y, null);
    }

    public LineBuilder<T> add(float x, float y, T user)
    {
        if (this.lines.isEmpty())
        {
            this.push();
        }

        Line line = this.lines.get(this.lines.size() - 1);

        line.add(x, y, user);

        return this;
    }

    public LineBuilder<T> push()
    {
        return this.push(new Line<>());
    }

    public LineBuilder<T> push(Line<T> line)
    {
        this.lines.add(line);

        return this;
    }

    public List<List<LinePoint<T>>> build()
    {
        List<List<LinePoint<T>>> output = new ArrayList<>();

        for (Line line : this.lines)
        {
            List<LinePoint<T>> compiled = line.build(this.thickness);

            if (!compiled.isEmpty())
            {
                output.add(compiled);
            }
        }

        return output;
    }

    public void render(Batcher2D batcher2D, ILineRenderer<T> renderer)
    {
        List<List<LinePoint<T>>> build = this.build();

        if (build.isEmpty())
        {
            return;
        }

        GuiQuadMesh mesh = new GuiQuadMesh();
        Matrix3x2fc matrix = batcher2D.getContext().pose();

        for (List<LinePoint<T>> points : build)
        {
            int size = points.size();

            if (size < 4)
            {
                continue;
            }

            for (int i = 0; i + 3 < size; i += 2)
            {
                renderer.render(mesh, matrix, points.get(i));
                renderer.render(mesh, matrix, points.get(i + 1));
                renderer.render(mesh, matrix, points.get(i + 3));
                renderer.render(mesh, matrix, points.get(i + 2));
            }
        }

        if (!mesh.isEmpty())
        {
            batcher2D.drawQuadMesh(mesh);
        }
    }
}
