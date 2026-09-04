# Continuidad del port de renderizado a Minecraft 1.21.11

Actualizado: 2026-09-04. Este documento describe trabajo **parcial**, no un port terminado.

## 1. Objetivo y condiciones

Migrar BBS CML desde su renderizado de 1.21.4 a Fabric 1.21.11 conservando sus funciones: modelos BOBJ/glTF, formas, partículas, selección por píxel, gizmos, cámaras, editor, efectos, overlays, captura de películas y compatibilidad con Iris/Sodium.

El usuario autoriza continuar implementando el port. No hace falta volver a preguntar si quiere que se continúe. No publicar, hacer push ni enviar mensajes a terceros sin autorización. Conservar los cambios del usuario.

Leer `AGENTS.md` y `CONTRIBUTING.md` antes de editar. Reglas especialmente relevantes:

- No modificar archivos de configuración de Gradle, wrapper ni versiones de dependencias.
- Java con llaves en línea nueva, `this.` en accesos de instancia, sin `var`, sufijos `F`, `D`, `L` en mayúsculas.
- Comentarios del código en inglés; dentro de métodos, usar `/* */`.
- Mantener cliente en `src/client/java`; lógica compartida/servidor en `src/main/java`.
- No reemplazar funciones por métodos vacíos, devoluciones constantes o excepciones para aparentar que el port compila.
- No desactivar mixins necesarios ni poner `require = 0` para ocultar incompatibilidades.
- Una compilación correcta no demuestra que el render funcione. Se requiere validación en Minecraft antes de considerar terminado el port.

## 2. Estado del repositorio

Al empezar esta continuación, `git status --short` estaba limpio y los cambios de la etapa anterior estaban guardados por el usuario en `65cf6ab07` (`Part 1`). Antecedentes:

| Referencia | Contenido |
| --- | --- |
| `3ae6058e2` | Base anterior identificada como 1.21.4 |
| `0d834cc96` | Port parcial inicial, `1.21.11 (remake v15)` |
| `65cf6ab07` | Primer bloque de esta conversación |

La continuación añade cambios sobre `Part 1`; consultar `git status` y `git diff` para saber si ya fueron incorporados a otro commit. No ejecutar reset, checkout de todo el árbol ni revertir cambios para recuperar la base.

Configuración observada: Minecraft `1.21.11`, Yarn `1.21.11+build.6`, Java 21, Fabric API `0.141.6+1.21.11`. Confirmar leyendo `gradle.properties` sin editarlo. Hay MUCHAS cachés de otras versiones en `.gradle`; no elegir un JAR simplemente porque es el primero que aparece.

El último build completo del cliente sigue fallando. Javac muestra 100 errores por su límite de diagnósticos: **eso no significa que queden exactamente 100 errores**. El código mezcla APIs antiguas con migraciones parciales y referencias a clases inexistentes. `compileJava` compartido estaba al día; eso tampoco prueba el arranque del servidor.

## 3. Cambios realizados y límites de su validación

### Etapa Part 1

- Actualizados imports de `VertexFormat` a `com.mojang.blaze3d.vertex` y de `GlStateManager` a `com.mojang.blaze3d.opengl` en los archivos encontrados.
- Migradas referencias de `ModelTransformationMode` a `net.minecraft.item.ItemDisplayContext`; sus valores usados y `isFirstPerson()` se contrastaron con el JAR.
- Reparada la coma faltante entre `YggdrasilServicesKeyInfoMixin` y `GuiRendererMixin` en `bbs.client.mixins.json`.
- `RenderLayerTextureOverrideMixin` ahora apunta a `RenderLayers`, con cinco descriptores explícitos contrastados mediante `javap`: `entityCutoutNoCull(Identifier, boolean)`, `entityCutout`, `entityTranslucent(Identifier, boolean)`, `itemEntityTranslucentCull`, `outlineNoCull`. El `require = 5` exige los cinco puntos.
- Implementado `graphics/PickerPreviewRenderState.java`: pipeline GUI propio para resaltar el ID seleccionado. `UV1` transporta los 24 bits del ID y `Color` el resaltado. La matriz se copia al construir el estado. No se usan uniforms globales mutables por vista.
- Migrados `picker_preview.vsh/.fsh` a GLSL 330 con `DynamicTransforms` y `Projection`. Eliminado su descriptor JSON obsoleto y su registro antiguo en `BBSShaders`; los demás shaders de `BBSShaders` siguen pendientes.
- Implementado el método antes vacío `Batcher2D.drawPickerPreview`. Sus tres consumidores usan la vista GPU de `StencilFormFramebuffer`, no la antigua textura GL auxiliar.
- `StencilFormFramebuffer` redirige los pases por `RenderSystem.outputColorTextureOverride/outputDepthTextureOverride` y restaura sus valores al salir. La lectura modifica sólo `GL_READ_FRAMEBUFFER`, lo restaura con `finally`, decodifica bytes RGBA y valida coordenadas. Corregido el borde superior en la conversión Y de `pickGUI`.
- `UIKeyframeDopeSheet` y `UIKeyframeGraph` envían geometría por `GuiQuadMesh` y la cola GUI. Eliminadas sus llamadas de dibujo inmediato; el gráfico tenía el envío final comentado.

Validación realizada: compilación aislada del estado de resaltado contra el JAR local; JSON válido; existencia de los cinco descriptores del mixin; compilación/enlace GLSL en OpenGL real y lectura de resultados para 8 IDs, incluidos 32768, 65535, 65536 y límites de 24 bits. GPU utilizada: NVIDIA GeForce RTX 5060. No se ejecutó el mod completo.

### Continuación posterior a Part 1

- Implementado `graphics/ModelPreviewRenderer.java`, que faltaba: color/profundidad mediante `SimpleFramebuffer`, UBO de proyección y niebla desactivada, selección de destinos GPU y restauración de proyección, luces, niebla y model-view al terminar, incluso si el dibujo del consumidor falla.
- `UIModelRenderer` usa la vista GPU del preview directamente en la GUI. Se retiraron los marcadores estáticos `ACTIVE/TEXTURE` que sólo estaban referenciados por ese mismo archivo. El estado `BBSRendering.renderingWorld` se conserva y restaura alrededor del preview.
- Añadido un overload de `Batcher2D.texturedBox` para `GpuTextureView`, con `TexturedQuadGuiElementRenderState` y copia de matriz. Evita registrar identificadores y wrappers GL para las texturas GPU de estos previews.
- Liberación del destino y UBOs al retirar el visor o su árbol de UI. Los recursos pueden recrearse al volver a añadir el elemento.
- `BackgroundRendererMixin` ahora apunta a `net.minecraft.client.render.fog.FogRenderer`. `getFogColor` conserva esos argumentos pero es un método de instancia, no estático.
- `ItemUseRenderState` llama al método público `LivingEntity.setLivingFlag`; se eliminó la referencia a un accessor inexistente.
- `PendingFilmLaunch` usa `LevelLoadingScreen`; el JAR objetivo ya no contiene `DownloadingTerrainScreen`.
- Incorporadas verificaciones reproducibles en `tools/port/validate_render_port.py` y `PickerShaderProbe.java`.

**Límite importante:** implementar el contenedor del preview no migra automáticamente sus renderizadores hijos. Las rutas que aún escriben por GL crudo, shader global o buffers antiguos deben adaptarse para dibujar al destino correcto. No afirmar que todos los previews, el picking o las películas funcionan ya.

## 4. Cómo verificar y obtener evidencia

En PowerShell, desde la raíz del repositorio:

```powershell
git status --short
git diff --check
.\gradlew.bat compileClientJava --console=plain *> build/port-1.21.11-compile.log
python tools/port/validate_render_port.py
python tools/port/validate_render_port.py --gpu
```

El script usa `JAVA_HOME`, el classpath de Loom y los JARs cacheados que corresponden a `gradle.properties`. Necesita haber ejecutado Gradle para preparar esas cachés y las clases compartidas. No descarga dependencias ni cambia Gradle. La opción `--gpu` crea una ventana OpenGL **oculta**, compila los shaders actuales y comprueba píxeles de resultado. Los archivos temporales quedan en `build/port-check`.

Las verificaciones aisladas desactivan el annotation processor y **no validan la aplicación de mixins**. Revisar descriptores sólo comprueba que existen los métodos. La prueba GPU del shader tampoco valida el orden de la cola de Minecraft, Iris, recarga de recursos ni todos los formatos de vértice del mod.

Cuando compile todo:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runClient
```

No hay suite automatizada general del mod. No declarar probado en juego algo que sólo se compiló. Capturar los resultados reales de las pruebas manuales.

## 5. Fuentes de API y diferencias que no deben confundirse

Prioridad: JAR nombrado de **1.21.11 con los mappings de este checkout**, sus shaders vanilla, fuentes oficiales de la misma versión y después la base 1.21.4 para entender el comportamiento del mod.

- [Fabric para 1.21.11](https://www.fabricmc.net/2025/12/05/12111.html)
- [Documentación de renderizado de mundo, versión 1.21.11](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.11/develop/rendering/world.md)
- [Renderizador de block entities, versión 1.21.11](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.11/develop/blocks/block-entity-renderer.md)

La documentación puede usar nombres Mojang; este proyecto usa Yarn. No copiar nombres de clases sin comprobar sus equivalencias. Evitar documentación por defecto de 26.x.

Ejemplo para inspeccionar una firma sin adivinar:

```powershell
$portJar = Get-ChildItem .gradle/loom-cache/minecraftMaven -Recurse -Filter '*1.21.11*build.6*.jar' |
    Where-Object { $_.Name -like 'minecraft-clientOnly*' -and $_.Name -notlike '*sources*' } |
    Select-Object -First 1
& "$env:JAVA_HOME/bin/javap.exe" -p -s -classpath $portJar.FullName net.minecraft.client.render.fog.FogRenderer
```

Hay varios JARs transformados por Loom; registrar el elegido al producir diagnósticos. Para métodos compartidos, añadir también el JAR `minecraft-common` correspondiente. Para analizar implementaciones, `javap -c -p` o fuentes de la misma versión; no confiar en fuentes antiguas por tener un nombre similar.

## 6. Arquitectura que debe completar la migración

### Pipelines, capas y pases

`RenderPipeline` describe shaders, formato, modo de dibujo, uniforms/samplers y estados de blend/depth/cull. `RenderLayer` combina un pipeline con configuración de texturas y destino mediante `RenderSetup`. Un `RenderPass` enlaza los recursos concretos y ejecuta el dibujo.

Cambiar `ShaderProgramKey` por `RenderPipeline` en una declaración no migra al consumidor. Deben coincidir formato de vértices, modo QUADS/TRIANGLES, shader, atributos, bloques uniformes, texturas y destino. El estado GL global antiguo no sustituye el estado declarado por un pipeline.

### Uniforms y shaders

Antes de migrar cada shader, inventariar todos sus uniforms, valores predeterminados y lugares donde se actualizan. Trasladar uniforms a bloques std140 o a atributos/estado por elemento cuando corresponda. Declarar `.withUniform(..., UniformType.UNIFORM_BUFFER)` no crea, rellena ni enlaza el buffer.

Comprobar tamaños, alineación y padding. `Std140Builder.get()` hace `flip()`; no añade automáticamente el padding final del bloque. El preview alinea su bloque Fog a 16 bytes explícitamente. Usar constantes de `GpuBuffer.USAGE_*`, no máscaras numéricas sin explicación.

No borrar todos los JSON de shaders: retirar sólo los descriptores cuyo registro y consumidores hayan sido migrados. Distinguir esos descriptores de otros formatos JSON de recursos. No cambiar el algoritmo de color, iluminación, deformación o glow durante el port salvo necesidad demostrada.

### Preparación y dibujo diferido

La GUI usa `Matrix3x2f` y estados de dibujo; el mundo/modelos siguen necesitando matrices 3D. Copiar matrices/datos que puedan cambiar antes del dibujo. No capturar un formulario, cámara o callback mutable y asumir que mantiene el mismo estado hasta ejecutar la cola.

Entidades y block entities emplean estados y `OrderedRenderCommandQueue`. Consultar la implementación vanilla para preparar datos y enviar comandos. Un estado que sólo almacena la entidad viva no completa la separación de fases. No sustituir renderizadores por stubs para satisfacer interfaces.

### Destinos y vida útil

Restaurar en `finally` proyección, luces, niebla, model-view, destinos y cualquier estado GL que la ruta cambie. Comprobar scope anidado y excepciones. No restaurar el framebuffer a 0 suponiendo que siempre era el anterior.

No cerrar una textura/vista que todavía vaya a leer un elemento GUI ya encolado. Revisar especialmente resize, cierre de paneles, varias vistas del mismo editor por frame y recarga de recursos. Las rutas GPU nuevas y los VAOs/FBOs GL antiguos aún conviven; localizar cada interacción explícitamente.

## 7. Pendientes priorizados

| Prioridad | Área | Archivos/pistas y resultado esperado |
| --- | --- | --- |
| 1 | Núcleo de shaders | `client/BBSShaders.java`, `forms/renderers/FormRenderer.java`, `cubic/render/vao/ModelVAORenderer.java`: eliminar dependencia funcional de shader global, migrar uniforms y consumidores conjuntamente. |
| 1 | Preview e ítems | `UIModelRenderer`, `UIModelEditorRenderer`: verificar integración del contenedor nuevo; `ItemRenderHelper` sigue sin existir. Diseñar su preparación con `ItemModelManager`/`ItemRenderState` y cola correcta, sin vaciar colas globales ajenas. |
| 1 | Block entities | `ModelBlockEntityRenderer` referencia `ModelBlockEntityRenderState` inexistente; su método de sombra contiene preparación sin envío de dibujo. Implementar estados/comandos reales, incluyendo sombras, transforms y formularios. |
| 1 | Mixin GUI incompleto | `GuiRendererMixin` referencia `BbsFormGuiElementRenderer` inexistente. Determinar qué flujo debe consumir ese renderer especial; no crear un renderer vacío ni quitar la inyección sólo para compilar. |
| 2 | Geometría inmediata | `BufferRenderer`, `ShaderProgramKeys`, `ShaderProgramKey` siguen en numerosos renderizadores. Portar recorridos completos de modelos, billboards, extrusión, shapes, fluidos, partículas y overlays. |
| 2 | Picking de geometría | El preview del ID está migrado; los shaders que escriben IDs y los callbacks de `CustomVertexConsumerProvider` siguen usando el modelo anterior en varias rutas. Preservar IDs reservados de gizmos 1–16 y formas/huesos desde 17. |
| 2 | Mixins de mundo/entidades | `RenderLayerMixin` aún apunta a `RenderPhase.startDrawing`; dispatchers y algunas inyecciones tienen nombres/descriptores antiguos. `EntityRenderManager`/`BlockEntityRenderManager` existen en el JAR nuevo, pero renombrar no adapta sus métodos. |
| 3 | Efectos y exportación | `UISubtitleRenderer`, `UIImageRenderer`, `ScreenEffectRenderer`, `ColorGradeRenderer`, `VideoRenderer`, `VideoRecorder`, `BBSRendering`: revisar composición, alpha, HDR, resize, viewport, motion blur y readback. |
| 3 | Iris/Sodium | Mixins opcionales, shaders parcheados, VAOs y overlays de emisión. Validar versiones exactas declaradas por el proyecto, tanto sin shaderpack como con uno. |
| 4 | Robustez | Recursos GPU, reload, pérdida/recreación de destinos, cierre/reapertura de editores, instancias múltiples, fallos a mitad de render y ejecución sin cliente. |

También quedan tipos de otras APIs por adaptar, por ejemplo `ArmorItem`, `BakedModel`, `Fog` y métodos antiguos de cámaras/gestores. Leer el log actualizado; no asumir que una búsqueda de imports detecta todos los problemas de comportamiento.

## 8. Criterios de aceptación manual

1. `build` pasa y `runClient` arranca sin fallos de mixins, access wideners ni compilación de shaders.
2. Dashboard y timeline: recorte, scroll, zoom, keyframes, handles Bézier, tooltips y vistas simultáneas.
3. Preview: abrir/cerrar/redimensionar varios visores; cámara, rejilla, transparencia, luces, modelos e ítems; ausencia de contaminación del mundo o del HUD.
4. Picking: gizmos 1–16, huesos/formas desde 17, selección/hover, distintos GUI scales y esquinas/bordes; IDs altos y geometría transparente.
5. Modelos: BOBJ/glTF, animaciones, materiales, escala negativa, normals, armaduras e ítems en manos; entidades morfeadas y bloques modelo.
6. Formas: billboards, extrusión, bloques/estructuras, labels, partículas, fluidos, trails y framebuffer forms; máscaras, tint y glow.
7. Películas: cámara, croma/niebla, overlays de imagen/subtítulos, reproducción y exportación; distintas resoluciones/FPS, motion blur y alpha.
8. Compatibilidad: vanilla/Fabric, Sodium, Iris sin shaderpack y con shaderpack. Revisar sombras, translucencia, glow y UI en cada modo.
9. Recarga de recursos y reapertura repetida de editores sin texturas obsoletas, errores GL ni crecimiento continuo de recursos.

Registrar caso, configuración, resultado y evidencia. No marcar un bloque como terminado porque desaparecieron sus primeros errores de compilación.

## 9. Prompt para continuar con otra IA

Copiar lo siguiente y dar acceso al repositorio completo; no basta con este documento ni con un fragmento del log.

```text
Continúa el port de BBS CML de Minecraft 1.21.4 a Fabric 1.21.11.

Primero lee AGENTS.md, CONTRIBUTING.md y docs/PORT_1_21_11_HANDOFF.md.
Revisa git status, git log reciente y git diff; conserva los cambios existentes.
El usuario autoriza implementar y verificar el port. No modifiques Gradle ni
dependencias. No hagas push ni contactes a terceros.

El port NO está completo. Ya hay un pipeline de resaltado de picking y migración
parcial de GUI, así como un contenedor GPU de previews. No rehagas esas piezas
sin encontrar un defecto. BBSShaders y muchos consumidores siguen usando APIs
de 1.21.4; quedan estados/clases faltantes y mixins con firmas antiguas.

Ejecuta compileClientJava, conserva el diagnóstico y verifica cada API en los
JARs Yarn de 1.21.11 correspondientes a gradle.properties. No inventes firmas
basándote en versiones próximas ni mezcles nombres Mojang/Yarn.

Trabaja por recorridos funcionales completos: preparación de estado, geometría,
pipeline, atributos, uniforms std140, samplers, destinos, envío de comandos,
composición y liberación/restauración de recursos. No uses métodos vacíos,
callbacks que ignoran parámetros, require=0 indiscriminado ni eliminación de
funciones para hacer compilar. No sustituyas todos los shaders por uno genérico.

Continúa con los bloqueos prioritarios documentados, incluyendo el registro y
consumidores de BBSShaders, ItemRenderHelper, estados de block entities y los
mixins/renderizadores especiales incompletos. Preserva iluminación, glow,
selección de huesos/gizmos, animación, overlays y compatibilidad Iris/Sodium.

Usa python tools/port/validate_render_port.py --gpu para verificar las piezas
ya cubiertas. Esa prueba aislada NO valida el mod completo ni los mixins en
ejecución. Cuando compile, ejecuta runClient y sigue la matriz manual del
documento. Informa con precisión qué se cambió, qué se probó y qué queda.

Actualiza docs/PORT_1_21_11_HANDOFF.md con las decisiones, pendientes y evidencia
al terminar cada bloque. No declares completo el port sin build y pruebas en
juego. Si necesitas entrada del usuario, explica el bloqueo concreto; no vuelvas
a pedir autorización genérica para continuar.
```
