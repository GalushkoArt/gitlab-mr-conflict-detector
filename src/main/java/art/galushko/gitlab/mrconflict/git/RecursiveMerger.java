package art.galushko.gitlab.mrconflict.git;

import org.eclipse.jgit.lib.Repository;

public class RecursiveMerger extends org.eclipse.jgit.merge.RecursiveMerger{
    protected RecursiveMerger(Repository local) {
        super(local);
    }
}
