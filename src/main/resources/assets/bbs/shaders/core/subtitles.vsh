#version 330

in vec3 Position;
in vec2 UV0;
in vec4 Color;

layout(std140) uniform SubtitleParameters
{
    mat4 SubtitleTransform;
    vec2 Blur;
    vec2 TextureSize;
};

out vec2 texCoord0;
out vec4 vertexColor;

void main()
{
    gl_Position = SubtitleTransform * vec4(Position, 1.0);

    texCoord0 = UV0;
    vertexColor = Color;
}
