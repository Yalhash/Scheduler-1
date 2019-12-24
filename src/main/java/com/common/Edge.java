package com.common;

import org.jgrapht.graph.DefaultEdge;
import static com.common.Utils.logInfo;

public class Edge extends DefaultEdge {

    private static final long serialVersionUID = 1L;

    public Edge() {
        super();
    }

    @Override
    protected Object getSource() {
        return super.getSource();
    }

    public Object getSourcePublic() {
        return this.getSource();
    }

    @Override
    protected Object getTarget() {
        return super.getTarget();
    }
    public Object getTargetPublic() {
        return this.getTarget();
    }
}
