package com.ops.ICmaps.Node;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface NodeRepository extends JpaRepository<Node, String> {

    List<Node> findByBlueLight(Boolean blueLight);

}
