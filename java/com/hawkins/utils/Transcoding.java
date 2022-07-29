package com.hawkins.utils;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

@Slf4j
public class Transcoding {

	public void transcode(Path FileIn, Path FileOut) throws IOException {

		// Path copied = null;
		

		FileStore store = Files.getFileStore(FileIn);
		log.info("FileStore for {} is of type {}", FileIn.toString(), store.type());
		log.info("Transcoding {} to {}", FileIn.toString(), FileOut.toString());

		/*
		 * if (store.type().contains("nfs4")) {
		 * 
		 * Copy the network file locally before transcoding
		 * 
		 * 
		 * copied = Paths.get("/tmp/" + FileIn.getFileName());
		 * 
		 * Files.copy(FileIn, copied, StandardCopyOption.REPLACE_EXISTING);
		 * 
		 * 
		 * }
		 */

		String inPathFile = FileIn.toString();
		String outPathFile = FileOut.toString();

		FFmpegProperties props = FFmpegProperties.getInstance();
		FFmpeg ffmpeg = props.getFfmpeg();
		FFprobe ffprobe = props.getFfprobe();

		FFmpegProbeResult probeResultInput = ffprobe.probe(inPathFile);

		FFmpegOutputBuilder outputBuilder = new FFmpegOutputBuilder();

		outputBuilder.setVideoCodec("h264_nvenc");
		outputBuilder.setFilename(outPathFile);
		// outputBuilder.setFilename(copied.toString());

		FFmpegBuilder builder = new FFmpegBuilder();

		builder.addInput(probeResultInput);
		builder.addOutput(outputBuilder);

		FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

		FFmpegJob job = executor.createJob(builder);
		job.run();

		/*
		 * FFmpegJob job = executor.createJob(builder, new ProgressListener() {
		 * 
		 * // Using the FFmpegProbeResult determine the duration of the input final
		 * double duration_ns = probeResultInput.getFormat().duration *
		 * TimeUnit.SECONDS.toNanos(1);
		 * 
		 * @Override public void progress(Progress progress) { double percentage =
		 * progress.out_time_ns / duration_ns;
		 * 
		 * // Print out interesting information about the progress
		 * System.out.println(String.format(
		 * "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx", percentage *
		 * 100, progress.status, progress.frame,
		 * FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
		 * progress.fps.doubleValue(), progress.speed )); } });
		 * 
		 * job.run();
		 */

	}
}
