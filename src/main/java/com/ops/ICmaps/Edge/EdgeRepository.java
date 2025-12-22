package com.ops.ICmaps.Edge;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, String> {

    Edge findByFromNodeAndToNode(String fromNode, String toNode);

    List<Edge> findByFromNode(String fromNode);

    List<Edge> findByToNode(String toNode);

    List<Edge> findByInterBuildingEdge(Boolean interBuildingEdge);
    List<Edge> findByBuildingId(String buildingId);

    @Query("select distinct e from Edge e left join fetch e.navModes")
    List<Edge> findAllWithNavModes();

}
