package mchorse.bbs_mod.cubic.model.gltf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root class for GLTF 2.0 data structure.
 */
public class GLTF
{
    public GLTFAsset asset;
    public int scene = -1;
    public List<GLTFScene> scenes = new ArrayList<>();
    public List<GLTFNode> nodes = new ArrayList<>();
    public List<GLTFMesh> meshes = new ArrayList<>();
    public List<GLTFAccessor> accessors = new ArrayList<>();
    public List<GLTFBufferView> bufferViews = new ArrayList<>();
    public List<GLTFBuffer> buffers = new ArrayList<>();
    public List<GLTFMaterial> materials = new ArrayList<>();
    public List<GLTFTexture> textures = new ArrayList<>();
    public List<GLTFImage> images = new ArrayList<>();
    public List<GLTFSkin> skins = new ArrayList<>();
    public List<GLTFAnimation> animations = new ArrayList<>();

    public static class GLTFAsset
    {
        public String version;
        public String generator;
    }

    public static class GLTFScene
    {
        public String name;
        public List<Integer> nodes = new ArrayList<>();
    }

    public static class GLTFNode
    {
        public String name;
        public int mesh = -1;
        public int skin = -1;
        public List<Integer> children = new ArrayList<>();
        public float[] matrix;
        public float[] translation;
        public float[] rotation;
        public float[] scale;
    }

    public static class GLTFMesh
    {
        public String name;
        public List<GLTFPrimitive> primitives = new ArrayList<>();
    }

    public static class GLTFPrimitive
    {
        public Map<String, Integer> attributes = new HashMap<>();
        public int indices = -1;
        public int material = -1;
        public int mode = 4; // TRIANGLES
    }

    public static class GLTFAccessor
    {
        public int bufferView = -1;
        public int byteOffset = 0;
        public int componentType;
        public boolean normalized;
        public int count;
        public String type; // SCALAR, VEC2, VEC3, VEC4, MAT4
        public float[] max;
        public float[] min;
    }

    public static class GLTFBufferView
    {
        public int buffer = -1;
        public int byteOffset = 0;
        public int byteLength;
        public int byteStride;
        public int target;
    }

    public static class GLTFBuffer
    {
        public int byteLength;
        public String uri;
        
        // Internal use for loaded data
        public byte[] data; 
    }

    public static class GLTFMaterial
    {
        public String name;
        public GLTFPBR pbrMetallicRoughness;
        public int normalTexture = -1;
        public int occlusionTexture = -1;
        public int emissiveTexture = -1;
        public float[] emissiveFactor;
        public String alphaMode = "OPAQUE";
        public float alphaCutoff = 0.5f;
        public boolean doubleSided;
    }

    public static class GLTFPBR
    {
        public float[] baseColorFactor = new float[] {1, 1, 1, 1};
        public GLTFTextureInfo baseColorTexture;
        public float metallicFactor = 1;
        public float roughnessFactor = 1;
        public GLTFTextureInfo metallicRoughnessTexture;
    }

    public static class GLTFTextureInfo
    {
        public int index = -1;
        public int texCoord = 0;
    }

    public static class GLTFTexture
    {
        public int sampler = -1;
        public int source = -1;
    }

    public static class GLTFImage
    {
        public String uri;
        public String mimeType;
        public int bufferView = -1;
    }

    public static class GLTFSkin
    {
        public int inverseBindMatrices = -1;
        public int skeleton = -1;
        public List<Integer> joints = new ArrayList<>();
    }

    public static class GLTFAnimation
    {
        public String name;
        public List<GLTFChannel> channels = new ArrayList<>();
        public List<GLTFSampler> samplers = new ArrayList<>();
    }

    public static class GLTFChannel
    {
        public int sampler = -1;
        public GLTFAnimationTarget target;
    }

    public static class GLTFAnimationTarget
    {
        public int node = -1;
        public String path;
    }

    public static class GLTFSampler
    {
        public int input = -1;
        public int output = -1;
        public String interpolation = "LINEAR";
    }
}
