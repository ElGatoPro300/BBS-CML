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

    float paintStrength = clamp(vertexColor.a, 0.0, 1.0);
    vec3 rgb = mix(tex.rgb, vertexColor.rgb, paintStrength);
    float alpha = tex.a * paintStrength;

    if (alpha < 0.01)
    {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
