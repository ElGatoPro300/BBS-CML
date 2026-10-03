#version 150

uniform sampler2D Sampler0;
uniform vec4 OutlineMaskColor;

in vec2 texCoord0;

out vec4 fragColor;

void main()
{
    float alpha = texture(Sampler0, texCoord0).a;

    if (alpha < 0.1)
    {
        discard;
    }

    fragColor = vec4(gl_FragCoord.z, 1.0, 0.0, 1.0);
}
