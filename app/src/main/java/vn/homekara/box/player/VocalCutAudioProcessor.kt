package vn.homekara.box.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bộ "tắt lời ca sĩ" gắn vào đường âm thanh của ExoPlayer.
 * Luôn hoạt động với PCM 16-bit stereo để bật/tắt tức thì; khi [enabled] = false thì chép nguyên văn.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile var enabled: Boolean = false

    private var scratch = ShortArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET   // định dạng khác: cho đi thẳng
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val out = replaceOutputBuffer(remaining)
        if (!enabled) {
            out.put(inputBuffer)
        } else {
            val shorts = remaining / 2
            if (scratch.size < shorts) scratch = ShortArray(shorts)
            inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer().get(scratch, 0, shorts)
            inputBuffer.position(inputBuffer.position() + shorts * 2)
            VocalCut.processInterleavedStereo(scratch, shorts)
            out.order(ByteOrder.nativeOrder()).asShortBuffer().put(scratch, 0, shorts)
            out.position(out.position() + shorts * 2)
            // byte lẻ (nếu có) chép nguyên
            while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        }
        out.flip()
    }
}
