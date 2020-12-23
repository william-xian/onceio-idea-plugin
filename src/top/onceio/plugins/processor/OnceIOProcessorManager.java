package top.onceio.plugins.processor;


import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;
import top.onceio.plugins.processor.clazz.model.ModelClassProcessor;
import top.onceio.plugins.processor.clazz.model.ModelPreDefinedInnerClassFieldProcessor;
import top.onceio.plugins.processor.clazz.model.ModelPreDefinedInnerClassMethodProcessor;
import top.onceio.plugins.processor.clazz.model.ModelProcessor;

import java.util.Arrays;
import java.util.Collection;

public class OnceIOProcessorManager {
    @NotNull
    public static Collection<Processor> getOnceIOProcessors() {
        return Arrays.asList(
                ServiceManager.getService(ModelPreDefinedInnerClassFieldProcessor.class),
                ServiceManager.getService(ModelPreDefinedInnerClassMethodProcessor.class),
                ServiceManager.getService(ModelClassProcessor.class),
                ServiceManager.getService(ModelProcessor.class)
        );
    }

}
