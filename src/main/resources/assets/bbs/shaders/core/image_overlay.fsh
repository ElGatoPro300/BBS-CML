#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform int BlendMode;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main()
{
    vec4 tex = texture(Sampler0, texCoord0);
    vec4 tint = vertexColor * ColorModulator;
    float alpha = tex.a * tint.a;

    if (alpha <= 0.0)
    {
        discard;
    }

    vec3 baseRgb = tex.rgb * tint.rgb;

    if (BlendMode == 0)
    {
        /* Normal: standard un-premultiplied output for standard alpha blending */
        fragColor = vec4(baseRgb, alpha);
    }
    else if (BlendMode == 7)
    {
        /* Overlay / Vivid Multiply: 2.0 * baseRgb * alpha */
        fragColor = vec4(2.0 * baseRgb * alpha, alpha);
    }
    else
    {
        /* Multiply, Screen, Add, Saturation, Incrustation, Exclusion, Color Dodge:
         * Premultiplying base RGB by alpha ensures both vertex/opacity alpha and
         * base PNG texture transparency smoothly blend with the background */
        fragColor = vec4(baseRgb * alpha, alpha);
    }
}
