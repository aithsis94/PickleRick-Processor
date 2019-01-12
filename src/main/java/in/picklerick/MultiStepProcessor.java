package in.picklerick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by ajitesh on 07/11/18.
 */

public class MultiStepProcessor implements IProcessStep {

    private List<IProcessStep> processStepList;

    public MultiStepProcessor() {
        this.processStepList = new ArrayList<>();
    }

    public MultiStepProcessor add(IProcessStep step) {
        this.processStepList.add(step);
        return this;
    }

    public MultiStepProcessor remove(IProcessStep step) {
        this.processStepList.remove(step);
        return this;
    }

    public MultiStepProcessor removeAllSteps() {
        this.processStepList.clear();
        return this;
    }

    public Collection<IProcessStep> getAllSteps() {
        return Collections.unmodifiableCollection(this.processStepList);
    }

    @Override
    public void process(ProcessVariables variables) throws Exception {

        for (IProcessStep step : this.processStepList) {
            step.process(variables);
        }
    }
}
