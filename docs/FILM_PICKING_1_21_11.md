# Film viewport picking in 1.21.11

The film editor renders picking IDs during GUI rendering into
`StencilFormFramebuffer`, then reads the pixel under the cursor. Alt picks a replay;
without Alt, the selected replay exposes its bones. Those existing controls remain
unchanged.

Two projection regressions prevented the picking pass from matching the world:

- `UIFilmPanel.lastProjection` copied the camera/view matrix instead of the world
  projection captured by `WorldRendererMixin`.
- `UIFilmController.renderPickingPreview` had its projection switch commented out,
  leaving the GUI projection active while rendering world-space picking geometry.

The panel now captures `BBSRendering.projection`. The picking pass uploads that
matrix through `BBSRendering.setProjectionMatrix` with `ProjectionType.PERSPECTIVE`;
the existing matrix cache restores the GUI projection afterward. Pixel readback now
uses `height - 1 - localY`, matching OpenGL's bottom-left origin without addressing
one row beyond the texture at the viewport's top edge.

Validation: the user confirmed model actor selection and bone selection work after
the projection correction. Structure/extruded actor selection and persistent gizmo
visibility still failed in that test.

The follow-up routes retained `ModelVAO` picking meshes through `ModelEffectPass`,
so structure and extrusion bind the picker uniform buffer and offscreen attachments
instead of issuing the old raw VAO draw. Vertices retain local coordinates and use
the supplied model-view; the light/bone offset is zero for these whole-form meshes.
Ordinary VAO rendering is unchanged.

The gizmo visual now uses a pooled offscreen target and a queued GUI image after
the film image. This prevents the deferred film image from covering the immediate
colored gizmo. Picking remains separate. `compileClientJava` passed and the test
client exited normally. The user confirmed structure selection, extruded selection,
and persistent gizmo visibility all work correctly. That confirmation does not
specify separate coverage of Iris and vanilla.

The earlier structure atlas correction is confirmed working by the user. Glow was
also confirmed white; exact shaderpack bloom parity was not established.
