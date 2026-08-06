#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;

void main()
{
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    /* Light RGB only — keep vertex alpha so soft opacity survives. */
    vec4 lightColor = texelFetch(Sampler2, UV2 / 16, 0);

    vertexColor = vec4(Color.rgb * lightColor.rgb, Color.a);
    texCoord0 = UV0;
}
