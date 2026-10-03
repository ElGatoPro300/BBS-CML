#version 150

uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform float Thickness;

in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    int radius = int(ceil(Thickness));

    if (radius <= 0)
    {
        fragColor = vec4(0.0, -1.0, 0.0, 1.0);
        return;
    }

    float bestDepth = 0.0;
    float bestDist = 1.0e6;
    bool found = false;

    for (int x = -radius; x <= radius; x++)
    {
        float dist = abs(float(x));

        if (dist > Thickness)
        {
            continue;
        }

        vec2 uv = texCoord0 + vec2(float(x), 0.0) * TexelSize;
        vec4 sampleColor = texture(Sampler0, uv);

        if (sampleColor.g > 0.5 && dist < bestDist)
        {
            bestDist = dist;
            bestDepth = sampleColor.r;
            found = true;
        }
    }

    fragColor = vec4(bestDepth, found ? bestDist : -1.0, 0.0, 1.0);
}
