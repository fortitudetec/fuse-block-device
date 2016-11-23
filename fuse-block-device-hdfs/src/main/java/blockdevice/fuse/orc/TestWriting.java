package blockdevice.fuse.orc;

import java.io.Closeable;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.OrcFile;
import org.apache.orc.OrcFile.WriterOptions;
import org.apache.orc.TypeDescription;
import org.apache.orc.Writer;

import com.google.common.io.Closer;

public class TestWriting {

  public static void main(String[] args) throws IOException {
    Configuration conf = new Configuration();
    TypeDescription schema = TypeDescription.fromString("struct<blockid:bigint,fileid:bigint,position:bigint>");
    WriterOptions writerOptions = OrcFile.writerOptions(conf).setSchema(schema);
    try (Closer closer = Closer.create()) {
      Writer writer = OrcFile.createWriter(new Path("./my-file.orc"), writerOptions);
      closer.register((Closeable) () -> writer.close());
      VectorizedRowBatch batch = schema.createRowBatch();
      LongColumnVector blockid = (LongColumnVector) batch.cols[0];
      LongColumnVector fileid = (LongColumnVector) batch.cols[1];
      LongColumnVector position = (LongColumnVector) batch.cols[2];
      for (int r = 0; r < 10000; ++r) {
        int row = batch.size++;
        blockid.vector[row] = r;
        fileid.vector[row] = r;
        position.vector[row] = r;
        // If the batch is full, write it out and start over.
        if (batch.size == batch.getMaxSize()) {
          writer.addRowBatch(batch);
          batch.reset();
        }
      }
    }
  }

  public static interface CloseableWriter extends Writer, Closeable {

  }
}
