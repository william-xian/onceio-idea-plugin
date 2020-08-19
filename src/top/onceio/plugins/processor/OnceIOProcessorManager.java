package top.onceio.plugins.processor;


import com.intellij.openapi.components.ServiceManager;
import de.plushnikov.intellij.plugin.processor.Processor;
import org.jetbrains.annotations.NotNull;
import top.onceio.plugins.processor.clazz.tbl.TblClassProcessor;
import top.onceio.plugins.processor.clazz.tbl.TblPreDefinedInnerClassFieldProcessor;
import top.onceio.plugins.processor.clazz.tbl.TblPreDefinedInnerClassMethodProcessor;
import top.onceio.plugins.processor.clazz.tbl.TblProcessor;

import java.util.Arrays;
import java.util.Collection;

public class OnceIOProcessorManager {
    @NotNull
    public static Collection<Processor> getLombokProcessors() {
        return Arrays.asList(
                ServiceManager.getService(TblPreDefinedInnerClassFieldProcessor.class),
                ServiceManager.getService(TblPreDefinedInnerClassMethodProcessor.class),
                ServiceManager.getService(TblClassProcessor.class),
                ServiceManager.getService(TblProcessor.class)
        );
    }

}
