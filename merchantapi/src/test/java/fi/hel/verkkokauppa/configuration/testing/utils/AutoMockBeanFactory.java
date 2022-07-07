package fi.hel.verkkokauppa.configuration.testing.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.beans.Introspector;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Mockito.mock;

@Slf4j
public class AutoMockBeanFactory extends DefaultListableBeanFactory {

    /**
     * If the bean can't be found, create a mock of the required type and register it
     *
     * @param beanName     The name of the bean that is being autowired.
     * @param requiredType The type of the bean that is being requested.
     * @param descriptor   The dependency descriptor.
     * @return A map of bean names to bean instances that match the required type.
     */
    @Override
    protected Map<String, Object> findAutowireCandidates(final String beanName, final Class<?> requiredType, final DependencyDescriptor descriptor) {
        String mockBeanName = Introspector.decapitalize(requiredType.getSimpleName()) + "Mock";
        Map<String, Object> autowireCandidates = new HashMap<>();
        try {
            autowireCandidates = super.findAutowireCandidates(beanName, requiredType, descriptor);
        } catch (UnsatisfiedDependencyException e) {
            if (e.getCause() != null && e.getCause().getCause() instanceof NoSuchBeanDefinitionException) {
                mockBeanName = ((NoSuchBeanDefinitionException) e.getCause().getCause()).getBeanName();
            }
            log.info("Automatically mocking bean : {}", mockBeanName);
            this.registerBeanDefinition(Objects.requireNonNull(mockBeanName), BeanDefinitionBuilder.genericBeanDefinition().getBeanDefinition());
        }
        if (autowireCandidates.isEmpty()) {
            final Object mock = mock(requiredType);
            autowireCandidates.put(mockBeanName, mock);
            this.addSingleton(mockBeanName, mock);
        }
        return autowireCandidates;
    }
}
