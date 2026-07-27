package mchorse.bbs_mod.cubic.model.gltf;

import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.cubic.model.gltf.GLTFConverter;
import mchorse.bbs_mod.cubic.model.gltf.GLTFParser;
import mchorse.bbs_mod.cubic.model.gltf.data.GLTF;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class GLTFTest
{
    public static void main(String[] args)
    {
        try
        {
            testSimpleBox();
            testSkinnedMesh();
            System.out.println("All tests passed!");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testSimpleBox() throws Exception
    {
        System.out.println("Testing Simple Box...");
        String json = "{" +
            "\"asset\": {\"version\": \"2.0\"}," +
            "\"nodes\": [{\"mesh\": 0}]," +
            "\"meshes\": [{" +
                "\"primitives\": [{" +
                    "\"attributes\": {\"POSITION\": 0}," +
                    "\"indices\": 1" +
                "}]" +
            "}]," +
            "\"accessors\": [" +
                "{\"bufferView\": 0, \"componentType\": 5126, \"count\": 3, \"type\": \"VEC3\"}," + // Position
                "{\"bufferView\": 1, \"componentType\": 5123, \"count\": 3, \"type\": \"SCALAR\"}" + // Indices
            "]," +
            "\"bufferViews\": [" +
                "{\"buffer\": 0, \"byteLength\": 36, \"byteOffset\": 0}," +
                "{\"buffer\": 0, \"byteLength\": 6, \"byteOffset\": 36}" +
            "]," +
            "\"buffers\": [{\"byteLength\": 42, \"uri\": \"data:application/octet-stream;base64,AAAAAAAAAAAAAIA/AAAAAAAAAAAAAIA/AAABAAIA\"}]" +
        "}";

        // Mock data: 3 floats (12 bytes) * 3 = 36 bytes. Indices: 3 shorts (6 bytes). Total 42.
        // Base64 is dummy.

        GLTF gltf = GLTFParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        
        // Populate dummy buffer data since parser might not decode data URI if not using GLTFModelLoader logic
        // Wait, GLTFParser doesn't decode data URIs. GLTFModelLoader does.
        // We need to manually decode for this test or mock it.
        if (gltf.buffers.get(0).uri != null)
        {
            // Simple mock buffer
            gltf.buffers.get(0).data = new byte[42]; 
        }

        BOBJLoader.BOBJData data = GLTFConverter.convert(gltf);
        
        if (data.meshes.isEmpty()) throw new RuntimeException("No meshes created");
        System.out.println("Box Mesh count: " + data.meshes.size());
    }

    private static void testSkinnedMesh() throws Exception
    {
        System.out.println("Testing Skinned Mesh...");
        // Minimal structure to trigger skinning path
        // Needs 3 vertices for a triangle
        String json = "{" +
            "\"asset\": {\"version\": \"2.0\"}," +
            "\"nodes\": [" +
                "{\"name\": \"Root\", \"children\": [1], \"skin\": 0, \"mesh\": 0}," + // Node 0
                "{\"name\": \"Joint1\"}" + // Node 1
            "]," +
            "\"skins\": [{\"joints\": [1]}]," +
            "\"meshes\": [{" +
                "\"primitives\": [{" +
                    "\"attributes\": {\"POSITION\": 0, \"JOINTS_0\": 1, \"WEIGHTS_0\": 2}," +
                    "\"indices\": 3" + // Added indices accessor
                "}]" +
            "}]," +
            "\"accessors\": [" +
                "{\"bufferView\": 0, \"componentType\": 5126, \"count\": 3, \"type\": \"VEC3\"}," + // POS (3 verts)
                "{\"bufferView\": 1, \"componentType\": 5121, \"count\": 3, \"type\": \"VEC4\"}," + // JOINTS (3 verts)
                "{\"bufferView\": 2, \"componentType\": 5126, \"count\": 3, \"type\": \"VEC4\"}," + // WEIGHTS (3 verts)
                "{\"bufferView\": 3, \"componentType\": 5123, \"count\": 3, \"type\": \"SCALAR\"}" + // INDICES (3 indices)
            "]," +
            "\"bufferViews\": [" +
                "{\"buffer\": 0, \"byteLength\": 36, \"byteOffset\": 0}," + // POS: 3 * 12 = 36
                "{\"buffer\": 0, \"byteLength\": 12, \"byteOffset\": 36}," + // JOINTS: 3 * 4 = 12
                "{\"buffer\": 0, \"byteLength\": 48, \"byteOffset\": 48}," + // WEIGHTS: 3 * 16 = 48
                "{\"buffer\": 0, \"byteLength\": 6, \"byteOffset\": 96}" +   // INDICES: 3 * 2 = 6
            "]," +
            "\"buffers\": [{\"byteLength\": 102}]" + // Total: 36+12+48+6 = 102
        "}";

        GLTF gltf = GLTFParser.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        gltf.buffers.get(0).data = new byte[102]; // Mock data

        BOBJLoader.BOBJData data = GLTFConverter.convert(gltf);
        
        BOBJLoader.BOBJMesh mesh = data.meshes.get(0);
        System.out.println("Skinned Mesh created: " + mesh.name);
        System.out.println("Faces: " + mesh.faces.size());
    }
}
