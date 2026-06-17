package no.nav.teamarbeidsforhold.githubapp.components;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

@Component
public final class GzipProcessor {
    public Flux<byte[]> gunzip(Flux<DataBuffer> source) {
        return DataBufferUtils.join(source)
                .<byte[]>flatMapMany(buffer -> Flux.using(
                        () -> new GZIPInputStream(buffer.asInputStream(true)),
                        gzip -> Flux.generate(sink -> {
                            byte[] buf = new byte[8192];
                            try {
                                int n = gzip.read(buf);
                                if (n == -1) {
                                    sink.complete();
                                } else {
                                    sink.next(Arrays.copyOf(buf, n));
                                }
                            } catch (IOException e) {
                                sink.error(e);
                            }
                        }),
                        gzipInputStream -> {
                            try {
                                gzipInputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
