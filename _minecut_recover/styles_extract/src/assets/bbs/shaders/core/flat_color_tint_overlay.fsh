#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec4 tex = texture(Sampler0, texCoord0);

    if (tex.a < 0.01)
    {
        discard;
    }

    /* vertexColor.rgb is already mix(1, FormColorTint, mask); multiply-blend in Java. */
    if (vertexColor.a < 0.001)
    {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, vertexColor.a);
}
