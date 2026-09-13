#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 TexelSize;
uniform float Thickness;
uniform vec4 OutlineMaskColor;

in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    vec4 self = texture(Sampler1, texCoord0);

    if (self.g > 0.5)
    {
        discard;
    }

    int radius = int(ceil(Thickness));

    if (radius <= 0)
    {
        discard;
    }

    float bestDist = 1.0e6;
    float bestDepth = 1.0;
    bool found = false;

    for (int y = -radius; y <= radius; y++)
    {
        float vdist = abs(float(y));

        if (vdist > Thickness)
        {
            continue;
        }

        vec2 uv = texCoord0 + vec2(0.0, float(y)) * TexelSize;
        vec4 sampleData = texture(Sampler0, uv);

        /* Negative .g means no mask pixel on this row within range */
        if (sampleData.g < 0.0)
        {
            continue;
        }

        float dist = length(vec2(sampleData.g, vdist));

        if (dist <= Thickness && dist < bestDist)
        {
            bestDist = dist;
            bestDepth = sampleData.r;
            found = true;
        }
    }

    if (!found)
    {
        discard;
    }

    float edgeFade = clamp(Thickness - bestDist + 1.0, 0.0, 1.0);

    fragColor = vec4(OutlineMaskColor.rgb, OutlineMaskColor.a * edgeFade);
}
