#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec4 texColor = texture(Sampler0, texCoord0);

    /* Keep empty glyph padding discarded; do not use vanilla's 0.1 cut which
     * kills soft label opacity below ~26/255. */
    if (texColor.a < 0.001)
    {
        discard;
    }

    vec4 color = texColor * vertexColor * ColorModulator;

    if (color.a < 0.001)
    {
        discard;
    }

    fragColor = color;
}
