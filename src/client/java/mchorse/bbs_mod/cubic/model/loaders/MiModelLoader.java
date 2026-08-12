package mchorse.bbs_mod.cubic.model.loaders;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelUV;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.InputStream;
import java.util.Collection;

public class MiModelLoader implements IModelLoader
{
    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link miLink = null;

        for (Link l : links)
        {
            if (l.path.endsWith(".mimodel"))
            {
                miLink = l;
                break;
            }
        }

        if (miLink == null)
        {
            for (Link l : links)
            {
                if (l.path.endsWith(".miobject"))
                {
                    miLink = l;
                    break;
                }
            }
        }

        if (miLink == null)
        {
            return null;
        }

        try (InputStream stream = models.provider.getAsset(miLink))
        {
            String text = IOUtils.readText(stream);
            BaseType data = DataToString.fromString(text);

            if (data.isMap())
            {
                MapType map = data.asMap();
                Model cubicModel = new Model(models.parser);
                Link texture = null;
                Pixels pixels = null;
                
                // Parse texture size
                if (map.has("texture_size"))
                {
                    ListType size = map.getList("texture_size");
                    if (size.size() >= 2)
                    {
                        cubicModel.textureWidth = size.getInt(0);
                        cubicModel.textureHeight = size.getInt(1);
                        System.out.println("[MiModelLoader] Texture size: " + cubicModel.textureWidth + "x" + cubicModel.textureHeight);
                    }
                }

                // Find texture
                if (map.has("texture"))
                {
                    String textureName = map.getString("texture");
                    System.out.println("[MiModelLoader] Looking for texture: " + textureName);
                    String tnLower = textureName.toLowerCase();
                    String tnBase = stripExtension(tnLower);

                    // Pass 1: direct endsWith match (keeps current behavior)
                    for (Link l : links)
                    {
                        if (l.path.toLowerCase().endsWith(textureName.toLowerCase()))
                        {
                            texture = l;
                            System.out.println("[MiModelLoader] Found texture by exact suffix: " + l.toString());
                            break;
                        }
                    }

                    if (texture == null)
                    {
                        for (Link l : links)
                        {
                            String lowerPath = l.path.toLowerCase();

                            if (lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg"))
                            {
                                String base = basename(lowerPath);
                                String baseNoExt = stripExtension(base);
                                if (base.equals(tnLower) || baseNoExt.equals(tnBase))
                                {
                                    texture = l;
                                    System.out.println("[MiModelLoader] Found texture by basename: " + l.toString());
                                    break;
                                }
                            }
                        }
                    }
                }
                
                if (texture == null)
                {
                    System.out.println("[MiModelLoader] Explicit texture not found. Searching for any image...");
                    for (Link l : links)
                    {
                        String path = l.path.toLowerCase();

                        if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg"))
                        {
                            texture = l;
                            System.out.println("[MiModelLoader] Using fallback texture: " + l.toString());
                            break;
                        }
                    }
                }
                
                if (texture == null)
                {
                    System.err.println("[MiModelLoader] CRITICAL: No texture found for model " + id + ". It might render black or invisible.");
                }
                else if (cubicModel.textureWidth > 0 && cubicModel.textureHeight > 0)
                {
                    try (InputStream texStream = models.provider.getAsset(texture))
                    {
                        pixels = Pixels.fromPNGStream(texStream);
                    }
                    catch (Exception e)
                    {
                        System.err.println("[MiModelLoader] Failed to read texture pixels for alpha extrusion: " + texture);
                        e.printStackTrace();
                    }
                }
                
                // Parse parts
                if (map.has("parts"))
                {
                    Vector3f rootOrigin = new Vector3f();

                    for (BaseType part : map.getList("parts"))
                    {
                        if (part.isMap())
                        {
                            ModelGroup group = this.parseGroup(part.asMap(), cubicModel, null, rootOrigin, pixels);
                            if (group != null)
                            {
                                cubicModel.topGroups.add(group);
                            }
                        }
                    }
                }

                if (cubicModel.topGroups.isEmpty()) {
                    System.err.println("[MiModelLoader] WARNING: No top-level groups loaded! Model will be invisible.");
                } else {
                    System.out.println("[MiModelLoader] Loaded " + cubicModel.topGroups.size() + " top-level groups.");
                }
                
                cubicModel.initialize();

                if (pixels != null)
                {
                    pixels.delete();
                }
                
                ModelInstance instance = new ModelInstance(id, cubicModel, new Animations(models.parser), texture);
                instance.applyConfig(config);
                
                return instance;
            }
        }
        catch (Exception e)
        {
            System.err.println("Failed to load .mimodel: " + miLink);
            e.printStackTrace();
        }

        return null;
    }

    private static String basename(String path)
    {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String stripExtension(String name)
    {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private Vector3f swapYZ(Vector3f v)
    {
        return new Vector3f(-v.x, v.y, -v.z);
    }

    private ModelGroup parseGroup(MapType data, Model model, ModelGroup parent, Vector3f parentOrigin, Pixels pixels)
    {
        String name = data.getString("name");
        System.out.println("[MiModelLoader] Parsing group: " + name);
        ModelGroup group = new ModelGroup(name);
        Vector3f parentOriginAbs = parentOrigin != null ? new Vector3f(parentOrigin) : new Vector3f();
        Vector3f localOrigin = new Vector3f();
        
        if (data.has("position"))
        {
            localOrigin.set(DataStorageUtils.vector3fFromData(data.getList("position")));
        }
        
        Vector3f groupOriginAbs = this.swapYZ(localOrigin).add(parentOriginAbs);
        group.initial.translate.set(groupOriginAbs);
        group.initial.pivot.set(groupOriginAbs);
        
        if (data.has("rotation"))
        {
             Vector3f rot = DataStorageUtils.vector3fFromData(data.getList("rotation"));
             group.initial.rotate.set(this.swapYZ(rot));
        }
        
        // Shapes (Cubes)
        if (data.has("shapes"))
        {
            for (BaseType shapeData : data.getList("shapes"))
            {
                if (shapeData.isMap())
                {
                    ModelCube cube = this.parseCube(shapeData.asMap(), model, groupOriginAbs, pixels);
                    if (cube != null)
                    {
                        group.cubes.add(cube);
                    }
                }
            }
        }
        
        // Children parts
        if (data.has("parts"))
        {
            for (BaseType part : data.getList("parts"))
            {
                if (part.isMap())
                {
                    ModelGroup child = this.parseGroup(part.asMap(), model, group, groupOriginAbs, pixels);
                    if (child != null)
                    {
                        group.children.add(child);
                    }
                }
            }
        }
        
        return group;
    }

    private ModelCube parseCube(MapType data, Model model, Vector3f groupOriginAbs, Pixels pixels)
    {
        String type = data.has("type") ? data.getString("type") : "unknown";

        ModelCube cube = new ModelCube();
        
        Vector3f from = new Vector3f();
        Vector3f to = new Vector3f();
        float planeDepthScale = 1F;
        boolean is3dPlane = false;
        boolean thinDetailPlane = false;
        ModelUV planeFace = null;
        
        if (data.has("from")) from.set(DataStorageUtils.vector3fFromData(data.getList("from")));
        if (data.has("to")) to.set(DataStorageUtils.vector3fFromData(data.getList("to")));
        
        float inflate = data.has("inflate") ? data.getFloat("inflate") : 0;
        if (inflate != 0)
        {
            from.sub(inflate, inflate, inflate);
            to.add(inflate, inflate, inflate);
        }

        if (data.has("scale"))
        {
            Vector3f scale = DataStorageUtils.vector3fFromData(data.getList("scale"));

            if (type.equals("plane"))
            {
                planeDepthScale = Math.abs(scale.z);
            }

            from.mul(scale);
            to.mul(scale);
        }
        
        Vector3f localFrom = this.swapYZ(from);
        Vector3f localTo = this.swapYZ(to);
        
        Vector3f cubeLocalPos = new Vector3f();
        if (data.has("position"))
        {
            cubeLocalPos.set(DataStorageUtils.vector3fFromData(data.getList("position")));
        }
        Vector3f cubeOriginAbs = this.swapYZ(cubeLocalPos).add(groupOriginAbs);
        
        Vector3f minLocal = new Vector3f(
            Math.min(localFrom.x, localTo.x),
            Math.min(localFrom.y, localTo.y),
            Math.min(localFrom.z, localTo.z)
        );
        Vector3f maxLocal = new Vector3f(
            Math.max(localFrom.x, localTo.x),
            Math.max(localFrom.y, localTo.y),
            Math.max(localFrom.z, localTo.z)
        );
        
        Vector3f min = new Vector3f(minLocal).add(cubeOriginAbs);
        Vector3f max = new Vector3f(maxLocal).add(cubeOriginAbs);
        
        cube.size.set(max).sub(min);

        if (cube.size.lengthSquared() < 0.0001f)
        {
            System.out.println("[MiModelLoader] WARNING: Cube has zero size! " + cube.size + " (Original from: " + from + ", to: " + to + ")");
        }
        
        if (data.has("rotation"))
        {
            cube.rotate.set(this.swapYZ(DataStorageUtils.vector3fFromData(data.getList("rotation"))));
        }
        
        if (data.has("uv"))
        {
            ListType uvList = data.getList("uv");
            if (uvList.size() >= 2)
            {
                Vector2f uv = new Vector2f(uvList.getFloat(0), uvList.getFloat(1));

                if (!type.equals("plane"))
                {
                    float depth = 0F;
                    if (data.has("from") && data.has("to"))
                    {
                        ListType fromData = data.getList("from");
                        ListType toData = data.getList("to");
                        if (fromData.size() >= 3 && toData.size() >= 3)
                        {
                            depth = Math.abs(toData.getFloat(2) - fromData.getFloat(2));
                        }
                    }

                    float dPixels = (float) Math.floor(depth);
                    uv.x -= dPixels;
                    uv.y -= dPixels;

                    boolean mirror = data.has("texture_mirror") && data.getBool("texture_mirror");
                    cube.setupBoxUV(uv, mirror);
                }
                else
                {
                    boolean is3d = data.has("3d") && data.getBool("3d");
                    boolean thinDetail = is3d && planeDepthScale < 0.2F;

                    is3dPlane = is3d;
                    thinDetailPlane = thinDetail;

                    float dx = Math.abs(to.x - from.x);
                    float dy = Math.abs(to.y - from.y);
                    float dz = Math.abs(to.z - from.z);

                    int thinAxis; // 0=X,1=Y,2=Z
                    float w;
                    float h;

                    if (dz < dx && dz < dy)
                    {
                        thinAxis = 2;
                        w = dx;
                        h = dy;
                    }
                    else if (dx < dy && dx < dz)
                    {
                        thinAxis = 0;
                        w = dz;
                        h = dy;
                    }
                    else
                    {
                        thinAxis = 1;
                        w = dx;
                        h = dz;
                    }

                    ModelUV face = new ModelUV();
                    face.origin.set(uv.x, uv.y);
                    face.size.set(w, h);

                    planeFace = face;

                    if (!is3d || thinDetail)
                    {
                        if (thinAxis == 2)
                        {
                            cube.front = face;
                            if (is3d) cube.back = face.copy();
                        }
                        else if (thinAxis == 0)
                        {
                            cube.left = face;
                            if (is3d) cube.right = face.copy();
                        }
                        else
                        {
                            cube.top = face;
                            if (is3d) cube.bottom = face.copy();
                        }
                    }
                    else
                    {
                        cube.front = face;
                        cube.back = face.copy();
                        cube.left = face.copy();
                        cube.right = face.copy();
                        cube.top = face.copy();
                        cube.bottom = face.copy();
                    }
                }
            }
        }
        
        /* Alpha-based culling for fully transparent 3D planes */
        if (type.equals("plane") && is3dPlane && !thinDetailPlane && pixels != null && planeFace != null)
        {
            int texW = pixels.width;
            int texH = pixels.height;

            int startX = Math.max(0, (int) planeFace.origin.x);
            int startY = Math.max(0, (int) planeFace.origin.y);
            int endX = Math.min(texW, (int) Math.ceil(planeFace.origin.x + planeFace.size.x));
            int endY = Math.min(texH, (int) Math.ceil(planeFace.origin.y + planeFace.size.y));

            boolean hasOpaque = false;

            for (int x = startX; x < endX && !hasOpaque; x++)
            {
                for (int y = startY; y < endY; y++)
                {
                    Color c = pixels.getColor(x, y);

                    if (c != null && c.a > 0.01F)
                    {
                        hasOpaque = true;

                        break;
                    }
                }
            }

            if (!hasOpaque)
            {
                return null;
            }
        }

        cube.origin.set(min);

        if (type.equals("plane"))
        {
            boolean is3d = data.has("3d") && data.getBool("3d");
            boolean thinDetail = is3d && planeDepthScale < 0.2F;
            float thickness = (is3d && !thinDetail) ? 1F : 0.01F;

            float sx = Math.abs(cube.size.x);
            float sy = Math.abs(cube.size.y);
            float sz = Math.abs(cube.size.z);

            int thinAxis = 0;
            float minSize = sx;

            if (sy < minSize)
            {
                thinAxis = 1;
                minSize = sy;
            }

            if (sz < minSize)
            {
                thinAxis = 2;
            }

            if (thinAxis == 0)
            {
                cube.origin.x -= thickness / 2F;
                cube.size.x = thickness;
            }
            else if (thinAxis == 1)
            {
                cube.origin.y -= thickness / 2F;
                cube.size.y = thickness;
            }
            else
            {
                cube.origin.z -= thickness / 2F;
                cube.size.z = thickness;
            }
        }
        
        cube.pivot.set(cubeOriginAbs);
        if (model.textureWidth > 0 && model.textureHeight > 0)
        {
            cube.generateQuads(model.textureWidth, model.textureHeight);
        }
        
        return cube;
    }
}
