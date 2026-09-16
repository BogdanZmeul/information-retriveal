package cluster;

import vector.DocumentVector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cluster implements Serializable {
    private final DocumentVector leader;
    private final List<DocumentVector> followers;

    public Cluster(DocumentVector leader) {
        this.leader = leader;
        this.followers = new ArrayList<>();
    }

    public void addFollower(DocumentVector follower) {
        followers.add(follower);
    }

    public DocumentVector getLeader() {
        return leader;
    }

    public List<DocumentVector> getFollowers() {
        return followers;
    }

    public List<DocumentVector> getAllDocuments() {
        List<DocumentVector> all = new ArrayList<>();
        all.add(leader);
        all.addAll(followers);
        return all;
    }
}