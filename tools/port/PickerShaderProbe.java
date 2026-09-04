import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;

public class PickerShaderProbe
{
    private static int compile(int type, String source)
    {
        int shader = GL33.glCreateShader(type);
        GL33.glShaderSource(shader, source);
        GL33.glCompileShader(shader);
        if (GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == 0)
        {
            throw new AssertionError(GL33.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public static void main(String[] args) throws Exception
    {
        if (!GLFW.glfwInit()) throw new AssertionError("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, 8);
        long window = GLFW.glfwCreateWindow(8, 8, "Picker shader validation", 0, 0);
        if (window == 0) throw new AssertionError("OpenGL context creation failed");
        try
        {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            int program = GL33.glCreateProgram();
            GL33.glAttachShader(program, compile(GL33.GL_VERTEX_SHADER, Files.readString(Path.of("build/port-check/picker_preview.vsh"))));
            GL33.glAttachShader(program, compile(GL33.GL_FRAGMENT_SHADER, Files.readString(Path.of("src/main/resources/assets/bbs/shaders/core/picker_preview.fsh"))));
            GL33.glLinkProgram(program);
            if (GL33.glGetProgrami(program, GL33.GL_LINK_STATUS) == 0) throw new AssertionError(GL33.glGetProgramInfoLog(program));
            GL33.glUseProgram(program);
            String[] blocks = {"DynamicTransforms", "Projection"};
            for (int binding = 0; binding < blocks.length; binding++)
            {
                int index = GL33.glGetUniformBlockIndex(program, blocks[binding]);
                int size = GL33.glGetActiveUniformBlocki(program, index, GL33.GL_UNIFORM_BLOCK_DATA_SIZE);
                ByteBuffer data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
                for (int i = 0; i < 4; i++) data.putFloat(i * 20, 1F);
                int buffer = GL33.glGenBuffers();
                GL33.glBindBuffer(GL33.GL_UNIFORM_BUFFER, buffer);
                GL33.glBufferData(GL33.GL_UNIFORM_BUFFER, data, GL33.GL_STATIC_DRAW);
                GL33.glUniformBlockBinding(program, index, binding);
                GL33.glBindBufferBase(GL33.GL_UNIFORM_BUFFER, binding, buffer);
            }
            GL33.glBindVertexArray(GL33.glGenVertexArrays());
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, GL33.glGenBuffers());
            GL33.glBufferData(GL33.GL_ARRAY_BUFFER, new float[] {-1F, -1F, 0F, 1F, -1F, 0F, 1F, 1F, 0F, -1F, 1F, 0F}, GL33.GL_STATIC_DRAW);
            int position = GL33.glGetAttribLocation(program, "Position");
            GL33.glEnableVertexAttribArray(position);
            GL33.glVertexAttribPointer(position, 3, GL33.GL_FLOAT, false, 12, 0L);
            GL33.glVertexAttrib2f(GL33.glGetAttribLocation(program, "UV0"), 0.5F, 0.5F);
            GL33.glVertexAttrib4f(GL33.glGetAttribLocation(program, "Color"), 64F / 255F, 128F / 255F, 192F / 255F, 1F);
            int targetAttribute = GL33.glGetAttribLocation(program, "UV1");
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, GL33.glGenTextures());
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
            GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
            GL33.glUniform1i(GL33.glGetUniformLocation(program, "Sampler0"), 0);
            GL33.glViewport(0, 0, 8, 8);
            GL33.glDisable(GL33.GL_DITHER);
            int[] targets = {1, 16, 17, 32768, 65535, 65536, 0xABCDEF, 0xFFFFFF};
            for (int target : targets)
            {
                ByteBuffer pixel = ByteBuffer.allocateDirect(4);
                pixel.put(0, (byte) target).put(1, (byte) (target >>> 8)).put(2, (byte) (target >>> 16)).put(3, (byte) 255);
                GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA8, 1, 1, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, pixel);
                GL33.glVertexAttribI2i(targetAttribute, (short) (target & 0xFFFF), target >>> 16);
                GL33.glClearColor(0F, 0F, 0F, 0F);
                GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);
                GL33.glDrawArrays(GL33.GL_TRIANGLE_FAN, 0, 4);
                GL33.glReadPixels(4, 4, 1, 1, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, pixel);
                int[] expected = target == 1 ? new int[] {255, 89, 89, 191} : target == 16 ? new int[] {255, 255, 255, 191} : new int[] {64, 128, 192, 255};
                for (int i = 0; i < 4; i++)
                {
                    if (Math.abs(Byte.toUnsignedInt(pixel.get(i)) - expected[i]) > 1) throw new AssertionError("Wrong highlight for ID " + target + " channel " + i);
                }
                GL33.glVertexAttribI2i(targetAttribute, 0, 0);
                GL33.glClear(GL33.GL_COLOR_BUFFER_BIT);
                GL33.glDrawArrays(GL33.GL_TRIANGLE_FAN, 0, 4);
                GL33.glReadPixels(4, 4, 1, 1, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, pixel);
                if (Byte.toUnsignedInt(pixel.get(3)) != 0) throw new AssertionError("Nonselected ID was not discarded");
            }
            if (GL33.glGetError() != GL33.GL_NO_ERROR) throw new AssertionError("OpenGL error");
            System.out.println("PASS: GLSL compile/link; 8 pick IDs including signed-short and 24-bit boundaries; highlight colors; nonselected fragments discarded. GPU: " + GL33.glGetString(GL33.GL_RENDERER));
        }
        finally
        {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
