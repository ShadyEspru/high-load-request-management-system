#!/usr/bin/env python3
"""Generate the final HLRMS diagram package.

The same geometry is emitted as editable diagrams.net XML and as SVG.  SVG is
then exported with Inkscape to PNG and PDF, so all three requested formats stay
visually aligned and can be regenerated from one source of truth.
"""

from __future__ import annotations

import argparse
import html
import math
import os
import subprocess
import textwrap
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, Sequence
from xml.etree import ElementTree as ET


WIDTH = 1920
HEIGHT = 1080
FONT = "DejaVu Sans"

PALETTE = {
    "core": ("#E8F0FE", "#2563EB", "#123B7A"),
    "edge": ("#EEF2FF", "#4F46E5", "#312E81"),
    "service": ("#ECFDF5", "#059669", "#065F46"),
    "messaging": ("#FFF7ED", "#EA580C", "#9A3412"),
    "data": ("#F5F3FF", "#7C3AED", "#4C1D95"),
    "security": ("#FEF2F2", "#DC2626", "#7F1D1D"),
    "external": ("#FFFBEB", "#D97706", "#78350F"),
    "ops": ("#ECFEFF", "#0891B2", "#155E75"),
    "neutral": ("#F8FAFC", "#64748B", "#1E293B"),
    "success": ("#F0FDF4", "#16A34A", "#14532D"),
    "failure": ("#FFF1F2", "#E11D48", "#881337"),
}

EDGE_COLOR = "#64748B"
MUTED = "#475569"
INK = "#0F172A"
GRID = "#E2E8F0"


@dataclass
class Node:
    id: str
    x: float
    y: float
    w: float
    h: float
    title: str
    subtitle: str = ""
    kind: str = "neutral"
    shape: str = "card"
    title_size: int = 21
    body_size: int = 15
    align: str = "center"
    dashed: bool = False
    parent: str = "1"


@dataclass
class Group:
    id: str
    x: float
    y: float
    w: float
    h: float
    title: str
    subtitle: str = ""
    kind: str = "neutral"
    dashed: bool = True


@dataclass
class Edge:
    id: str
    source: str | None
    target: str | None
    label: str = ""
    color: str = EDGE_COLOR
    dashed: bool = False
    width: float = 2.2
    points: list[tuple[float, float]] = field(default_factory=list)
    arrow: bool = True
    label_dx: float = 0
    label_dy: float = 0
    label_size: int = 13


@dataclass
class TextItem:
    id: str
    x: float
    y: float
    w: float
    h: float
    text: str
    size: int = 18
    color: str = MUTED
    bold: bool = False
    align: str = "left"


class Diagram:
    def __init__(self, key: str, title: str, subtitle: str, section: str):
        self.key = key
        self.title = title
        self.subtitle = subtitle
        self.section = section
        self.nodes: list[Node] = []
        self.groups: list[Group] = []
        self.edges: list[Edge] = []
        self.texts: list[TextItem] = []
        self.legend: list[tuple[str, str]] = []
        self.note = ""

    def group(self, *args, **kwargs) -> Group:
        item = Group(*args, **kwargs)
        self.groups.append(item)
        return item

    def node(self, *args, **kwargs) -> Node:
        item = Node(*args, **kwargs)
        self.nodes.append(item)
        return item

    def edge(self, *args, **kwargs) -> Edge:
        item = Edge(*args, **kwargs)
        self.edges.append(item)
        return item

    def text(self, *args, **kwargs) -> TextItem:
        item = TextItem(*args, **kwargs)
        self.texts.append(item)
        return item

    def set_legend(self, *items: tuple[str, str]) -> None:
        self.legend = list(items)


def p(kind: str) -> tuple[str, str, str]:
    return PALETTE[kind]


def escape(text: str) -> str:
    return html.escape(text, quote=True)


def wrap_lines(text: str, width: float, font_size: int, max_lines: int = 5) -> list[str]:
    if not text:
        return []
    lines: list[str] = []
    approx = max(10, int(width / max(7.5, font_size * 0.56)))
    for paragraph in text.split("\n"):
        if not paragraph:
            lines.append("")
            continue
        lines.extend(textwrap.wrap(paragraph, width=approx, break_long_words=False, break_on_hyphens=False) or [paragraph])
    if len(lines) > max_lines:
        lines = lines[:max_lines]
        lines[-1] = lines[-1].rstrip(" .") + "…"
    return lines


def node_index(diagram: Diagram) -> dict[str, Node]:
    return {n.id: n for n in diagram.nodes}


def anchor(a: Node, b: Node) -> tuple[tuple[float, float], tuple[float, float]]:
    ax, ay = a.x + a.w / 2, a.y + a.h / 2
    bx, by = b.x + b.w / 2, b.y + b.h / 2
    dx, dy = bx - ax, by - ay
    if abs(dx) >= abs(dy):
        if dx >= 0:
            return (a.x + a.w, ay), (b.x, by)
        return (a.x, ay), (b.x + b.w, by)
    if dy >= 0:
        return (ax, a.y + a.h), (bx, b.y)
    return (ax, a.y), (bx, b.y + b.h)


def edge_points(edge: Edge, nodes: dict[str, Node]) -> list[tuple[float, float]]:
    if edge.points:
        return edge.points
    if edge.source is None or edge.target is None:
        raise ValueError(f"Free edge {edge.id} requires explicit points")
    start, end = anchor(nodes[edge.source], nodes[edge.target])
    sx, sy = start
    tx, ty = end
    if abs(tx - sx) >= abs(ty - sy):
        mx = (sx + tx) / 2
        return [start, (mx, sy), (mx, ty), end]
    my = (sy + ty) / 2
    return [start, (sx, my), (tx, my), end]


def longest_segment(
    points: Sequence[tuple[float, float]],
) -> tuple[tuple[float, float], tuple[float, float]]:
    if len(points) < 2:
        return points[0], points[0]
    best = (points[0], points[1])
    best_len = -1.0
    for a, b in zip(points, points[1:]):
        length = math.hypot(b[0] - a[0], b[1] - a[1])
        if length > best_len:
            best = (a, b)
            best_len = length
    return best


def longest_segment_midpoint(points: Sequence[tuple[float, float]]) -> tuple[float, float]:
    a, b = longest_segment(points)
    return ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)


def polyline_midpoint(points: Sequence[tuple[float, float]]) -> tuple[float, float]:
    lengths = [math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(points, points[1:])]
    total = sum(lengths)
    if total == 0:
        return points[0]
    remaining = total / 2
    for (a, b), length in zip(zip(points, points[1:]), lengths):
        if remaining <= length:
            ratio = remaining / length if length else 0
            return (a[0] + (b[0] - a[0]) * ratio, a[1] + (b[1] - a[1]) * ratio)
        remaining -= length
    return points[-1]


def edge_label_layout(
    edge: Edge,
    points: Sequence[tuple[float, float]],
) -> tuple[float, float, list[str]]:
    """Place relationship text beside the line without an opaque label box."""
    label_lines = wrap_lines(edge.label, 340, edge.label_size, 3)
    estimated_width = max(
        24.0,
        max((len(line) for line in label_lines), default=1) * edge.label_size * 0.56,
    )
    a, b = longest_segment(points)
    lx, ly = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
    if abs(b[0] - a[0]) >= abs(b[1] - a[1]):
        ly -= edge.label_size + 5
    else:
        direction = -1 if lx + estimated_width + 24 > WIDTH - 45 else 1
        lx += direction * (estimated_width / 2 + 12)
        ly += edge.label_size / 2 - 2
    return lx + edge.label_dx, ly + edge.label_dy, label_lines


def svg_text(parent: ET.Element, x: float, y: float, text: str, size: int, color: str,
             bold: bool = False, anchor_name: str = "start") -> ET.Element:
    element = ET.SubElement(parent, "text", {
        "x": str(x), "y": str(y), "fill": color,
        "font-family": FONT, "font-size": str(size),
        "font-weight": "700" if bold else "400",
        "text-anchor": anchor_name,
    })
    element.text = text
    return element


def render_node_svg(parent: ET.Element, node: Node) -> None:
    fill, stroke, text_color = p(node.kind)
    g = ET.SubElement(parent, "g", {"id": f"node-{node.id}"})
    if node.shape == "diamond":
        pts = [
            (node.x + node.w / 2, node.y),
            (node.x + node.w, node.y + node.h / 2),
            (node.x + node.w / 2, node.y + node.h),
            (node.x, node.y + node.h / 2),
        ]
        ET.SubElement(g, "polygon", {
            "points": " ".join(f"{x},{y}" for x, y in pts),
            "fill": fill, "stroke": stroke, "stroke-width": "2.5",
            "stroke-dasharray": "9 7" if node.dashed else "none",
            "filter": "url(#shadow)",
        })
    elif node.shape == "database":
        ET.SubElement(g, "rect", {
            "x": str(node.x), "y": str(node.y + 13), "width": str(node.w),
            "height": str(node.h - 26), "fill": fill, "stroke": stroke,
            "stroke-width": "2.5", "filter": "url(#shadow)",
        })
        ET.SubElement(g, "ellipse", {
            "cx": str(node.x + node.w / 2), "cy": str(node.y + 13),
            "rx": str(node.w / 2), "ry": "13", "fill": fill,
            "stroke": stroke, "stroke-width": "2.5",
        })
        ET.SubElement(g, "path", {
            "d": f"M {node.x} {node.y + node.h - 13} A {node.w/2} 13 0 0 0 {node.x + node.w} {node.y + node.h - 13}",
            "fill": "none", "stroke": stroke, "stroke-width": "2.5",
        })
    elif node.shape == "queue":
        ET.SubElement(g, "rect", {
            "x": str(node.x), "y": str(node.y), "width": str(node.w),
            "height": str(node.h), "rx": "22", "fill": fill,
            "stroke": stroke, "stroke-width": "2.5", "filter": "url(#shadow)",
        })
        for offset in (22, 34):
            ET.SubElement(g, "line", {
                "x1": str(node.x + offset), "x2": str(node.x + offset),
                "y1": str(node.y + 18), "y2": str(node.y + node.h - 18),
                "stroke": stroke, "stroke-width": "2", "opacity": "0.55",
            })
    else:
        radius = "32" if node.shape in {"pill", "state"} else "18"
        ET.SubElement(g, "rect", {
            "x": str(node.x), "y": str(node.y), "width": str(node.w),
            "height": str(node.h), "rx": radius, "fill": fill,
            "stroke": stroke, "stroke-width": "2.5",
            "stroke-dasharray": "9 7" if node.dashed else "none",
            "filter": "url(#shadow)",
        })
    if node.shape not in {"diamond", "state", "pill"}:
        ET.SubElement(g, "rect", {
            "x": str(node.x), "y": str(node.y), "width": "8",
            "height": str(node.h), "rx": "4", "fill": stroke, "stroke": "none",
        })

    title_lines = wrap_lines(node.title, node.w - 34, node.title_size, 3)
    body_lines = wrap_lines(node.subtitle, node.w - 38, node.body_size, 5)
    line_gap = 5
    total = len(title_lines) * (node.title_size + line_gap)
    if body_lines:
        total += 8 + len(body_lines) * (node.body_size + 4)
    start_y = node.y + max(24, (node.h - total) / 2 + node.title_size)
    if node.shape == "database":
        start_y = max(start_y, node.y + 13 + node.title_size + 7)
    tx = node.x + (node.w / 2 if node.align == "center" else 24)
    anch = "middle" if node.align == "center" else "start"
    for line in title_lines:
        svg_text(g, tx, start_y, line, node.title_size, text_color, True, anch)
        start_y += node.title_size + line_gap
    if body_lines:
        start_y += 3
        for line in body_lines:
            svg_text(g, tx, start_y, line, node.body_size, MUTED, False, anch)
            start_y += node.body_size + 4


def render_svg(diagram: Diagram, output: Path) -> None:
    ET.register_namespace("", "http://www.w3.org/2000/svg")
    root = ET.Element("svg", {
        "xmlns": "http://www.w3.org/2000/svg",
        "width": str(WIDTH), "height": str(HEIGHT),
        "viewBox": f"0 0 {WIDTH} {HEIGHT}",
    })
    defs = ET.SubElement(root, "defs")
    marker = ET.SubElement(defs, "marker", {
        "id": "arrow", "viewBox": "0 0 10 10", "refX": "9", "refY": "5",
        "markerWidth": "7", "markerHeight": "7", "orient": "auto-start-reverse",
    })
    ET.SubElement(marker, "path", {"d": "M 0 0 L 10 5 L 0 10 z", "fill": EDGE_COLOR})
    filt = ET.SubElement(defs, "filter", {"id": "shadow", "x": "-20%", "y": "-20%", "width": "140%", "height": "150%"})
    ET.SubElement(filt, "feDropShadow", {"dx": "0", "dy": "4", "stdDeviation": "5", "flood-color": "#0F172A", "flood-opacity": "0.09"})

    ET.SubElement(root, "rect", {"x": "0", "y": "0", "width": str(WIDTH), "height": str(HEIGHT), "fill": "#FFFFFF"})
    svg_text(root, 70, 46, f"HLRMS · {diagram.section.upper()}", 16, "#2563EB", True)
    svg_text(root, 70, 88, diagram.title, 35, INK, True)
    svg_text(root, 70, 119, diagram.subtitle, 17, MUTED)
    ET.SubElement(root, "line", {"x1": "70", "x2": "1850", "y1": "139", "y2": "139", "stroke": GRID, "stroke-width": "2"})

    for group in diagram.groups:
        fill, stroke, text_color = p(group.kind)
        ET.SubElement(root, "rect", {
            "x": str(group.x), "y": str(group.y), "width": str(group.w), "height": str(group.h),
            "rx": "24", "fill": fill, "fill-opacity": "0.38", "stroke": stroke,
            "stroke-width": "2.5", "stroke-dasharray": "10 8" if group.dashed else "none",
        })
        svg_text(root, group.x + 22, group.y + 30, group.title, 18, text_color, True)
        if group.subtitle:
            svg_text(root, group.x + 22, group.y + 54, group.subtitle, 14, MUTED)

    nodes = node_index(diagram)
    edge_layer = ET.SubElement(root, "g", {"id": "edges"})
    for edge in diagram.edges:
        points = edge_points(edge, nodes)
        path_d = "M " + " L ".join(f"{x} {y}" for x, y in points)
        attrs = {
            "d": path_d, "fill": "none", "stroke": edge.color,
            "stroke-width": str(edge.width), "stroke-linejoin": "round",
            "stroke-linecap": "round",
        }
        if edge.dashed:
            attrs["stroke-dasharray"] = "9 7"
        if edge.arrow:
            attrs["marker-end"] = "url(#arrow)"
        ET.SubElement(edge_layer, "path", attrs)
        if edge.label:
            lx, ly, label_lines = edge_label_layout(edge, points)
            ty = ly
            for line in label_lines:
                svg_text(edge_layer, lx, ty, line, edge.label_size, MUTED, False, "middle")
                ty += edge.label_size + 4

    for node in diagram.nodes:
        render_node_svg(root, node)

    for item in diagram.texts:
        lines = wrap_lines(item.text, item.w, item.size, max_lines=10)
        tx = item.x if item.align == "left" else item.x + item.w / 2
        anch = "start" if item.align == "left" else "middle"
        ty = item.y + item.size
        for line in lines:
            svg_text(root, tx, ty, line, item.size, item.color, item.bold, anch)
            ty += item.size + 5

    footer_y = 1034
    if diagram.legend:
        x = 70
        for label, kind in diagram.legend:
            fill, stroke, _ = p(kind)
            ET.SubElement(root, "rect", {"x": str(x), "y": str(footer_y - 13), "width": "18", "height": "18", "rx": "4", "fill": fill, "stroke": stroke, "stroke-width": "2"})
            svg_text(root, x + 27, footer_y + 1, label, 14, MUTED)
            x += 42 + len(label) * 8.2
    if diagram.note:
        svg_text(root, 1850, footer_y + 1, diagram.note, 14, MUTED, False, "end")

    output.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(output, encoding="utf-8", xml_declaration=True)


def drawio_style_for_node(node: Node) -> str:
    fill, stroke, text_color = p(node.kind)
    shape = "rounded=1;arcSize=18;"
    if node.shape == "database":
        shape = "shape=cylinder3;boundedLbl=1;backgroundOutline=1;size=15;"
    elif node.shape == "queue":
        shape = "shape=mxgraph.basic.rect;rounded=1;"
    elif node.shape == "diamond":
        shape = "rhombus;"
    elif node.shape in {"pill", "state"}:
        shape = "rounded=1;arcSize=45;"
    return (
        f"{shape}whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
        f"fontColor={text_color};fontFamily={FONT};fontSize={node.title_size};fontStyle=1;"
        f"align={node.align};verticalAlign=middle;spacing=12;strokeWidth=2;"
        + ("dashed=1;dashPattern=9 7;" if node.dashed else "")
    )


def render_drawio(diagram: Diagram, output: Path) -> None:
    modified = datetime.now(timezone.utc).isoformat(timespec="seconds")
    mxfile = ET.Element("mxfile", {
        "host": "app.diagrams.net", "modified": modified,
        "agent": "HLRMS diagram generator", "version": "24.7.17",
        "type": "device", "compressed": "false",
    })
    page = ET.SubElement(mxfile, "diagram", {"id": diagram.key.replace("/", "-"), "name": "Page-1"})
    model = ET.SubElement(page, "mxGraphModel", {
        "dx": "1920", "dy": "1080", "grid": "1", "gridSize": "10",
        "guides": "1", "tooltips": "1", "connect": "1", "arrows": "1",
        "fold": "1", "page": "1", "pageScale": "1", "pageWidth": str(WIDTH),
        "pageHeight": str(HEIGHT), "math": "0", "shadow": "0", "background": "#FFFFFF",
    })
    root = ET.SubElement(model, "root")
    ET.SubElement(root, "mxCell", {"id": "0"})
    ET.SubElement(root, "mxCell", {"id": "1", "parent": "0"})

    def vertex(cell_id: str, value: str, style: str, x: float, y: float, w: float, h: float) -> None:
        cell = ET.SubElement(root, "mxCell", {"id": cell_id, "value": value, "style": style, "vertex": "1", "parent": "1"})
        ET.SubElement(cell, "mxGeometry", {"x": str(x), "y": str(y), "width": str(w), "height": str(h), "as": "geometry"})

    vertex("diagram-eyebrow", escape(f"HLRMS · {diagram.section.upper()}"),
           f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontFamily={FONT};fontSize=16;fontStyle=1;fontColor=#2563EB;", 70, 18, 900, 36)
    vertex("diagram-title", escape(diagram.title),
           f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontFamily={FONT};fontSize=35;fontStyle=1;fontColor={INK};", 70, 52, 1500, 46)
    vertex("diagram-subtitle", escape(diagram.subtitle),
           f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontFamily={FONT};fontSize=17;fontColor={MUTED};", 70, 96, 1700, 34)
    line = ET.SubElement(root, "mxCell", {"id": "diagram-rule", "style": f"endArrow=none;html=1;strokeColor={GRID};strokeWidth=2;", "edge": "1", "parent": "1"})
    geom = ET.SubElement(line, "mxGeometry", {"relative": "1", "as": "geometry"})
    ET.SubElement(geom, "mxPoint", {"x": "70", "y": "139", "as": "sourcePoint"})
    ET.SubElement(geom, "mxPoint", {"x": "1850", "y": "139", "as": "targetPoint"})

    for group in diagram.groups:
        fill, stroke, text_color = p(group.kind)
        value = f"<b>{escape(group.title)}</b>"
        if group.subtitle:
            value += f"<br><font style=\"font-size:14px;color:{MUTED}\">{escape(group.subtitle)}</font>"
        style = (
            f"rounded=1;arcSize=18;whiteSpace=wrap;html=1;fillColor={fill};fillOpacity=38;"
            f"strokeColor={stroke};fontColor={text_color};fontFamily={FONT};fontSize=18;fontStyle=1;"
            "align=left;verticalAlign=top;spacingTop=12;spacingLeft=14;strokeWidth=2;"
            + ("dashed=1;dashPattern=10 8;" if group.dashed else "")
        )
        vertex(f"group-{group.id}", value, style, group.x, group.y, group.w, group.h)

    for node in diagram.nodes:
        value = f"<b>{escape(node.title)}</b>"
        if node.subtitle:
            value += f"<br><font style=\"font-size:{node.body_size}px;color:{MUTED}\">{escape(node.subtitle).replace(chr(10), '<br>')}</font>"
        vertex(node.id, value, drawio_style_for_node(node), node.x, node.y, node.w, node.h)

    nodes = node_index(diagram)
    for edge in diagram.edges:
        points = edge_points(edge, nodes)
        attrs = {
            "id": edge.id, "value": escape(edge.label),
            "style": (
                f"edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;"
                f"strokeColor={edge.color};strokeWidth={edge.width};fontFamily={FONT};fontSize={edge.label_size};"
                f"fontColor={MUTED};fontStyle=0;labelBackgroundColor=none;labelBorderColor=none;"
                + ("dashed=1;dashPattern=9 7;" if edge.dashed else "")
                + ("endArrow=block;endFill=1;" if edge.arrow else "endArrow=none;")
            ),
            "edge": "1", "parent": "1",
        }
        if edge.source:
            attrs["source"] = edge.source
        if edge.target:
            attrs["target"] = edge.target
        cell = ET.SubElement(root, "mxCell", attrs)
        geometry = ET.SubElement(cell, "mxGeometry", {"relative": "1", "as": "geometry"})
        if edge.label:
            label_x, label_y, _ = edge_label_layout(edge, points)
            base_x, base_y = polyline_midpoint(points)
            ET.SubElement(geometry, "mxPoint", {
                "x": str(label_x - base_x),
                "y": str(label_y - base_y),
                "as": "offset",
            })
        if edge.points:
            ET.SubElement(geometry, "mxPoint", {"x": str(edge.points[0][0]), "y": str(edge.points[0][1]), "as": "sourcePoint"})
            ET.SubElement(geometry, "mxPoint", {"x": str(edge.points[-1][0]), "y": str(edge.points[-1][1]), "as": "targetPoint"})
            if len(edge.points) > 2:
                array = ET.SubElement(geometry, "Array", {"as": "points"})
                for x, y in edge.points[1:-1]:
                    ET.SubElement(array, "mxPoint", {"x": str(x), "y": str(y)})

    for item in diagram.texts:
        vertex(item.id, escape(item.text).replace("\n", "<br>"),
               f"text;html=1;strokeColor=none;fillColor=none;align={item.align};verticalAlign=top;fontFamily={FONT};fontSize={item.size};fontStyle={1 if item.bold else 0};fontColor={item.color};whiteSpace=wrap;",
               item.x, item.y, item.w, item.h)

    legend_x = 70
    for idx, (label, kind) in enumerate(diagram.legend):
        fill, stroke, _ = p(kind)
        vertex(f"legend-box-{idx}", "", f"rounded=1;arcSize=25;fillColor={fill};strokeColor={stroke};strokeWidth=2;", legend_x, 1018, 18, 18)
        vertex(f"legend-label-{idx}", escape(label), f"text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontFamily={FONT};fontSize=14;fontColor={MUTED};", legend_x + 25, 1012, max(80, len(label) * 8.4), 30)
        legend_x += 42 + len(label) * 8.2
    if diagram.note:
        vertex("diagram-note", escape(diagram.note), f"text;html=1;strokeColor=none;fillColor=none;align=right;verticalAlign=middle;fontFamily={FONT};fontSize=14;fontColor={MUTED};", 1120, 1012, 730, 30)

    output.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(mxfile).write(output, encoding="utf-8", xml_declaration=True)


def wait_for_complete_export(path: Path, kind: str, timeout: float = 10.0) -> None:
    """Wait for Inkscape's delegated writer to finish before an atomic rename."""
    deadline = time.monotonic() + timeout
    last_state: tuple[int, int] | None = None
    stable_checks = 0
    while time.monotonic() < deadline:
        try:
            stat = path.stat()
            state = (stat.st_size, stat.st_mtime_ns)
            with path.open("rb") as handle:
                handle.seek(max(0, stat.st_size - 64))
                tail = handle.read()
            complete = (
                tail.endswith(b"\x00\x00\x00\x00IEND\xaeB`\x82")
                if kind == "png"
                else b"%%EOF" in tail
            )
            stable_checks = stable_checks + 1 if complete and state == last_state else 0
            last_state = state
            if stable_checks >= 4:
                return
        except (FileNotFoundError, OSError):
            last_state = None
            stable_checks = 0
        time.sleep(0.05)
    raise RuntimeError(f"Timed out waiting for complete {kind.upper()} export: {path}")


def export(svg: Path, png: Path, pdf: Path) -> None:
    png.parent.mkdir(parents=True, exist_ok=True)
    png_tmp = png.with_name(f".{png.stem}.tmp.png")
    pdf_tmp = pdf.with_name(f".{pdf.stem}.tmp.pdf")
    try:
        subprocess.run([
            "inkscape", str(svg), "--export-type=png", f"--export-filename={png_tmp}",
            "--export-width=1920", "--export-height=1080", "--export-background=#FFFFFF",
        ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        wait_for_complete_export(png_tmp, "png")
        subprocess.run([
            "inkscape", str(svg), "--export-type=pdf", f"--export-filename={pdf_tmp}",
            "--export-background=#FFFFFF",
        ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        wait_for_complete_export(pdf_tmp, "pdf")
        os.replace(png_tmp, png)
        os.replace(pdf_tmp, pdf)
    finally:
        png_tmp.unlink(missing_ok=True)
        pdf_tmp.unlink(missing_ok=True)


def n(diagram: Diagram, node_id: str, x: float, y: float, w: float, h: float,
      title: str, subtitle: str = "", kind: str = "neutral", shape: str = "card",
      title_size: int = 22, body_size: int = 16, align: str = "center", dashed: bool = False) -> Node:
    return diagram.node(node_id, x, y, w, h, title, subtitle, kind, shape, title_size, body_size, align, dashed)


def e(diagram: Diagram, edge_id: str, source: str | None, target: str | None,
      label: str = "", *, color: str = EDGE_COLOR, dashed: bool = False,
      points: Iterable[tuple[float, float]] = (), arrow: bool = True,
      label_dx: float = 0, label_dy: float = 0, label_size: int = 13) -> Edge:
    return diagram.edge(
        edge_id, source, target, label, color, dashed, 2.2,
        list(points), arrow, label_dx, label_dy, label_size,
    )


# ---------------------------------------------------------------------------
# Diagram specifications
# ---------------------------------------------------------------------------


def finish(diagram: Diagram, note: str = "") -> Diagram:
    diagram.note = note
    if not diagram.legend:
        diagram.set_legend(
            ("Core platform", "core"),
            ("Runtime service", "service"),
            ("Messaging", "messaging"),
            ("Data store", "data"),
            ("External / replaceable", "external"),
        )
    return diagram


def lifeline(diagram: Diagram, node_id: str, x: float, title: str, subtitle: str, kind: str) -> None:
    n(diagram, node_id, x - 110, 168, 220, 92, title, subtitle, kind, title_size=19, body_size=14)
    e(diagram, f"life-{node_id}", None, None, "", color="#94A3B8", dashed=True,
      points=[(x, 260), (x, 952)], arrow=False)


def message(diagram: Diagram, edge_id: str, x1: float, x2: float, y: float, label: str,
            *, dashed: bool = False, color: str = EDGE_COLOR, reverse: bool = False) -> None:
    points = [(x2, y), (x1, y)] if reverse else [(x1, y), (x2, y)]
    e(diagram, edge_id, None, None, label, color=color, dashed=dashed, points=points)


def diagram_system_context() -> Diagram:
    d = Diagram(
        "01-context/system-context",
        "System Context",
        "A reusable high-load request platform between client systems and protected business processing.",
        "Context",
    )
    d.group("core-boundary", 560, 242, 800, 580, "HLRMS Core Platform",
            "Application-agnostic request admission, buffering, execution and traceability", "core", False)
    n(d, "gateway", 650, 340, 280, 120, "Unified API Entry",
      "Authentication · rate limiting · routing · correlation", "edge")
    n(d, "pipeline", 990, 340, 280, 120, "Reliable Request Pipeline",
      "Idempotency · persistence · outbox · status API", "core")
    n(d, "messaging", 650, 540, 280, 120, "Asynchronous Processing",
      "RabbitMQ · retry · DLQ · parallel Workers", "messaging")
    n(d, "observability", 990, 540, 280, 120, "Operational Visibility",
      "Prometheus metrics · Grafana dashboards · health checks", "ops")
    n(d, "clients", 90, 315, 330, 135, "Client Applications",
      "Mobile app · web portal · partner API · public service", "external")
    n(d, "users", 90, 545, 330, 120, "End Users",
      "Submit domain requests and track their status", "external")
    n(d, "operator", 1490, 315, 330, 135, "System Operator / Administrator",
      "Observe health, manage requests and investigate failures", "ops")
    n(d, "business", 1490, 545, 330, 135, "Replaceable Business Service",
      "Optional domain-specific execution outside HLRMS Core", "external", dashed=True)
    e(d, "ctx-1", "users", "clients", "uses",
      points=[(255, 545), (255, 450)])
    e(d, "ctx-2", "clients", "gateway", "JWT + Idempotency-Key",
      points=[(420, 385), (650, 385)])
    e(d, "ctx-3", "gateway", "pipeline", "",
      points=[(930, 400), (990, 400)])
    e(d, "ctx-4", "pipeline", "messaging", "",
      points=[(1130, 460), (1130, 500), (790, 500), (790, 540)])
    e(d, "ctx-5", "operator", "observability", "dashboards",
      points=[(1490, 382), (1410, 382), (1410, 600), (1270, 600)], label_dx=18)
    e(d, "ctx-6", "messaging", "business", "optional command",
      points=[(790, 660), (790, 735), (1490, 735), (1490, 612)])
    e(d, "ctx-7", "pipeline", "clients", "Request ID + status", dashed=True,
      points=[(1130, 340), (1130, 205), (470, 205), (470, 380), (420, 380)])
    d.set_legend(("HLRMS Core", "core"), ("Edge and access", "edge"),
                 ("Operations", "ops"), ("Replaceable client/domain", "external"))
    return finish(d)


def diagram_use_case_overview() -> Diagram:
    d = Diagram(
        "02-use-cases/use-case-overview",
        "Use Case Overview",
        "Platform capabilities are separated from any client application's business domain.",
        "Requirements",
    )
    d.group("use-cases", 450, 180, 1000, 760, "HLRMS Platform Use Cases", "Core platform behavior", "core", False)
    n(d, "uc-auth", 540, 250, 400, 100, "Authenticate",
      "Register · login · refresh access token", "security", shape="pill")
    n(d, "uc-submit", 540, 390, 400, 100, "Submit Request",
      "Validate · deduplicate · accept quickly", "core", shape="pill")
    n(d, "uc-track", 540, 530, 400, 100, "Track Request",
      "Read status and processing result", "service", shape="pill")
    n(d, "uc-list", 540, 670, 400, 100, "List Own Requests",
      "Ownership-isolated paginated history", "service", shape="pill")
    n(d, "uc-admin", 1030, 300, 340, 110, "Administer Requests",
      "RBAC-protected inspection", "security", shape="pill")
    n(d, "uc-observe", 1030, 480, 340, 110, "Observe Platform",
      "Metrics · health · queue · failures", "ops", shape="pill")
    n(d, "uc-test", 1030, 660, 340, 110, "Validate Performance",
      "Load · spike · soak · recovery", "ops", shape="pill")
    n(d, "actor-client", 70, 420, 300, 160, "Client Actors",
      "End User · mobile · web · partner system", "external")
    n(d, "actor-admin", 1540, 280, 320, 130, "Administrator",
      "Authorized operational access", "ops", title_size=20)
    n(d, "actor-perf", 1540, 650, 320, 130, "Performance Engineer",
      "Runs repeatable k6 scenarios", "ops", title_size=20)
    for idx, (target, target_y) in enumerate((("uc-auth", 300), ("uc-submit", 440), ("uc-track", 580), ("uc-list", 720)), 1):
        e(d, f"uco-client-{idx}", "actor-client", target, "", color="#94A3B8", arrow=False,
          points=[(370, 500), (430, 500), (430, target_y), (540, target_y)])
    e(d, "uco-admin-1", "actor-admin", "uc-admin", "", color="#94A3B8", arrow=False,
      points=[(1540, 345), (1370, 355)])
    e(d, "uco-admin-2", "actor-admin", "uc-observe", "", color="#94A3B8", arrow=False,
      points=[(1540, 345), (1495, 345), (1495, 535), (1370, 535)])
    e(d, "uco-perf", "actor-perf", "uc-test", "", color="#94A3B8", arrow=False,
      points=[(1540, 715), (1370, 715)])
    d.set_legend(("User capability", "service"), ("Security / RBAC", "security"),
                 ("Operational validation", "ops"), ("External actor", "external"))
    return finish(d)


def diagram_submit_use_case() -> Diagram:
    d = Diagram(
        "02-use-cases/use-case-submit-request",
        "Submit Request — Detailed Use Case",
        "The client receives a durable Request ID without waiting for business execution to finish.",
        "Requirements",
    )
    n(d, "caller", 60, 410, 290, 145, "Authenticated Caller",
      "JWT identity · payload · Idempotency-Key", "external")
    d.group("system", 430, 180, 1350, 760, "HLRMS Core", "UC-REQ-01 · asynchronous admission", "core", False)
    n(d, "main", 500, 390, 300, 135, "Submit Request",
      "POST /api/v1/requests", "core", shape="pill", title_size=24)
    n(d, "validate", 880, 230, 320, 105, "1 · Validate Input",
      "JWT identity · schema · request type", "security", shape="pill")
    n(d, "idem", 880, 385, 320, 105, "2 · Enforce Idempotency",
      "User-scoped key and payload fingerprint", "service", shape="pill")
    n(d, "persist", 880, 540, 320, 105, "3 · Persist Atomically",
      "Request row + Outbox event", "data", shape="pill")
    n(d, "response", 880, 695, 320, 105, "4 · Return Acceptance",
      "201 Created + Request ID + PENDING", "success", shape="pill")
    n(d, "conflict", 1380, 300, 310, 115, "Conflict Response",
      "Same key + different payload → 409", "failure", shape="pill")
    n(d, "replay", 1380, 500, 310, 115, "Replay Response",
      "Same key + same payload → original Request ID", "external", shape="pill")
    e(d, "ucs-1", "caller", "main", "POST request",
      points=[(350, 475), (500, 475)])
    e(d, "ucs-2", "main", "validate", "",
      points=[(800, 458), (840, 458), (840, 282), (880, 282)])
    e(d, "ucs-3", "validate", "idem", "", points=[(1040, 335), (1040, 385)])
    e(d, "ucs-4", "idem", "persist", "", points=[(1040, 490), (1040, 540)])
    e(d, "ucs-5", "persist", "response", "", points=[(1040, 645), (1040, 695)])
    e(d, "ucs-conflict", "idem", "conflict", "",
      points=[(1200, 438), (1290, 438), (1290, 358), (1380, 358)])
    e(d, "ucs-replay", "idem", "replay", "",
      points=[(1200, 438), (1320, 438), (1320, 558), (1380, 558)])
    e(d, "ucs-return", "response", "caller", "201 + Request ID", dashed=True,
      points=[(1040, 800), (1040, 825), (205, 825), (205, 555)])
    d.text("pre", 500, 855, 520, 75,
           "Preconditions: valid identity; supported request type; reachable durable database.", 16, MUTED, True)
    d.text("post", 1090, 855, 560, 75,
           "Postcondition: the request and its outbox event exist in one committed transaction.", 16, MUTED, True)
    d.set_legend(("Primary use case", "core"), ("Included behavior", "service"),
                 ("Alternative result", "external"), ("Error result", "failure"))
    return finish(d)


def diagram_dfd_level0() -> Diagram:
    d = Diagram(
        "03-dfd/dfd-level0-context",
        "Data Flow Diagram — Level 0",
        "External data flows through HLRMS without exposing its internal processes.",
        "Data Flow",
    )
    n(d, "client", 90, 330, 340, 145, "Client System",
      "Identity credentials · request payload · idempotency key", "external")
    n(d, "user", 90, 630, 340, 120, "End User", "Uses mobile, web or partner software", "external")
    n(d, "hlrms", 690, 300, 540, 410, "0 · HLRMS",
      "Authenticate callers\nAccept and trace requests\nBuffer work safely\nExecute asynchronously\nExpose status and operational metrics",
      "core", title_size=30, body_size=18)
    n(d, "operator", 1490, 285, 330, 135, "Operator / Administrator",
      "Health queries · administrative filters", "ops")
    n(d, "domain", 1490, 595, 330, 145, "Replaceable Business Service",
      "Optional execution command and result", "external", dashed=True)
    e(d, "d0-1", "user", "client", "interaction", points=[(260, 630), (260, 475)])
    e(d, "d0-2", "client", "hlrms", "credentials + request data",
      points=[(430, 380), (690, 380)])
    e(d, "d0-3", "hlrms", "client", "token · Request ID · status", dashed=True,
      points=[(960, 300), (960, 230), (500, 230), (500, 430), (430, 430)])
    e(d, "d0-4", "operator", "hlrms", "admin queries",
      points=[(1490, 330), (1230, 330)])
    e(d, "d0-5", "hlrms", "operator", "health + metrics", dashed=True,
      points=[(1230, 400), (1490, 400)])
    e(d, "d0-6", "hlrms", "domain", "optional command", dashed=True,
      points=[(1230, 620), (1490, 620)])
    e(d, "d0-7", "domain", "hlrms", "business result", dashed=True,
      points=[(1490, 700), (1230, 700)])
    d.set_legend(("External entity", "external"), ("System process", "core"),
                 ("Operational actor", "ops"), ("Optional flow", "neutral"))
    return finish(d)


def diagram_dfd_level1() -> Diagram:
    d = Diagram(
        "03-dfd/dfd-level1",
        "Data Flow Diagram — Level 1",
        "Authentication, admission, durable messaging, processing and status retrieval are independent flows.",
        "Data Flow",
    )
    n(d, "client", 55, 400, 230, 120, "Client System", "Credentials and requests", "external")
    n(d, "p1", 345, 190, 260, 105, "1.0 Authenticate", "Issue and validate JWT", "security")
    n(d, "authdb", 700, 180, 280, 130, "D1 · Auth Database",
      "Users · roles · refresh tokens", "data", "database", 20, 14)
    n(d, "p2", 345, 400, 260, 105, "2.0 Admit Request", "Validate and deduplicate", "core")
    n(d, "p3", 700, 400, 260, 105, "3.0 Persist Work", "Request + Outbox transaction", "data")
    n(d, "p4", 1080, 400, 260, 105, "4.0 Publish Event", "Claim and publish pending Outbox", "messaging")
    n(d, "rabbit", 1450, 180, 280, 130, "D4 · RabbitMQ",
      "Processing queue · retry · DLQ", "messaging", "queue", 20, 14)
    n(d, "p5", 1460, 400, 260, 105, "5.0 Process Work", "Consume, execute and update status", "service")
    n(d, "redis", 340, 685, 270, 130, "D2 · Redis",
      "Locks · cache · idempotency fast path", "data", "database", 20, 14)
    n(d, "p6", 700, 700, 260, 105, "6.0 Query Status", "Ownership-aware read", "service")
    n(d, "reqdb", 1070, 685, 280, 130, "D3 · Requests Database",
      "Requests · outbox · processed events", "data", "database", 20, 14)
    e(d, "d1-1", "client", "p1", "F1",
      points=[(285, 435), (315, 435), (315, 242), (345, 242)])
    e(d, "d1-2", "p1", "authdb", "F2", points=[(605, 242), (700, 242)])
    e(d, "d1-3", "client", "p2", "F3", points=[(285, 460), (345, 460)])
    e(d, "d1-4", "p2", "redis", "F4", points=[(475, 505), (475, 685)])
    e(d, "d1-5", "p2", "p3", "F5", points=[(605, 452), (700, 452)])
    e(d, "d1-6", "p3", "reqdb", "F6",
      points=[(830, 505), (830, 620), (1020, 620), (1020, 710), (1070, 710)])
    e(d, "d1-7", "p4", "reqdb", "F7", points=[(1210, 505), (1210, 685)])
    e(d, "d1-8", "p4", "rabbit", "F8",
      points=[(1210, 400), (1210, 340), (1410, 340), (1410, 245), (1450, 245)])
    e(d, "d1-9", "rabbit", "p5", "F9", points=[(1590, 310), (1590, 400)])
    e(d, "d1-10", "p5", "reqdb", "F10",
      points=[(1590, 505), (1590, 750), (1350, 750)])
    e(d, "d1-11", "client", "p6", "F11",
      points=[(170, 520), (170, 900), (830, 900), (830, 805)])
    e(d, "d1-12", "p6", "reqdb", "F12", points=[(960, 752), (1070, 752)])
    e(d, "d1-13", "p6", "client", "F13", dashed=True,
      points=[(700, 752), (650, 752), (650, 850), (300, 850), (300, 480), (285, 480)])
    d.text("d1-flow-note", 1040, 885, 700, 45,
           "F1–F13 are defined in the accompanying Data Dictionary.", 17, MUTED, True, "center")
    d.set_legend(("Process", "core"), ("Security process", "security"),
                 ("Data store", "data"), ("Message store", "messaging"))
    return finish(d)


def diagram_activity() -> Diagram:
    d = Diagram(
        "04-activity/request-processing-activity",
        "Request Processing Activity",
        "Synchronous admission ends at acceptance; durable asynchronous execution continues independently.",
        "Behavior",
    )
    for gid, y, title, kind in (
        ("lane-client", 165, "CLIENT", "external"),
        ("lane-edge", 340, "GATEWAY + REQUEST SERVICE", "core"),
        ("lane-async", 565, "OUTBOX + RABBITMQ", "messaging"),
        ("lane-worker", 790, "WORKER", "service"),
    ):
        d.group(gid, 55, y, 1810, 155 if gid != "lane-edge" else 190, title, "", kind, False)
    n(d, "a-start", 95, 210, 190, 70, "1 · Submit", "JWT + payload + key", "external", "pill", 18, 13)
    n(d, "a-receive", 365, 390, 235, 85, "2 · Authenticate", "Validate identity and schema", "security", "pill", 17, 12)
    n(d, "a-idem", 670, 390, 220, 85, "3 · Idempotency", "Replay or conflict", "service", "diamond", 16, 12)
    n(d, "a-commit", 965, 390, 245, 85, "4 · Atomic Commit", "Request + Outbox", "data", "pill", 17, 12)
    n(d, "a-accept", 1290, 210, 225, 70, "5 · 201 Accepted", "Request ID · PENDING", "success", "pill", 18, 13)
    n(d, "a-publish", 365, 610, 235, 85, "6 · Claim Batch", "Single publisher role", "messaging", "pill", 17, 12)
    n(d, "a-queue", 720, 610, 230, 85, "7 · Publish", "Confirm request.created", "messaging", "pill", 17, 12)
    n(d, "a-buffer", 1070, 610, 235, 85, "8 · Durable Queue", "Retry · redelivery · DLQ", "messaging", "queue", 17, 12)
    n(d, "a-consume", 365, 835, 235, 85, "9 · Consume", "Register event idempotently", "service", "pill", 17, 12)
    n(d, "a-process", 720, 835, 235, 85, "10 · Process", "PENDING → PROCESSING", "service", "pill", 17, 12)
    n(d, "a-final", 1070, 835, 235, 85, "11 · Persist Result", "COMPLETED or FAILED", "success", "pill", 17, 12)
    n(d, "a-dlq", 1420, 835, 235, 85, "Dead Letter Queue", "After retry exhaustion", "failure", "queue", 17, 12)
    # Explicit orthogonal routes keep each transition out of the activity cards
    # and avoid the doubled/crossing segments produced by automatic anchoring.
    e(d, "act-1", "a-start", "a-receive", "",
      points=[(285, 245), (325, 245), (325, 432), (365, 432)])
    e(d, "act-2", "a-receive", "a-idem", "",
      points=[(600, 432), (670, 432)])
    e(d, "act-3", "a-idem", "a-commit", "",
      points=[(890, 432), (965, 432)])
    e(d, "act-4", "a-commit", "a-accept", "",
      points=[(1210, 432), (1250, 432), (1250, 245), (1290, 245)])
    e(d, "act-5", "a-commit", "a-publish", "",
      points=[(1087, 475), (1087, 545), (482, 545), (482, 610)])
    e(d, "act-6", "a-publish", "a-queue", "",
      points=[(600, 652), (720, 652)])
    e(d, "act-7", "a-queue", "a-buffer", "",
      points=[(950, 652), (1070, 652)])
    e(d, "act-8", "a-buffer", "a-consume", "",
      points=[(1187, 695), (1187, 755), (482, 755), (482, 835)])
    e(d, "act-9", "a-consume", "a-process", "",
      points=[(600, 877), (720, 877)])
    e(d, "act-10", "a-process", "a-final", "",
      points=[(955, 877), (1070, 877)])
    e(d, "act-11", "a-process", "a-dlq", "", color="#E11D48",
      points=[(837, 920), (837, 940), (1537, 940), (1537, 920)])
    d.set_legend(("Synchronous admission", "core"), ("Durable asynchronous path", "messaging"),
                 ("Successful state", "success"), ("Failure isolation", "failure"))
    return finish(d)


def diagram_submission_sequence() -> Diagram:
    d = Diagram(
        "05-sequence/request-submission-sequence",
        "Request Submission Sequence",
        "The durable database transaction completes before the client receives the Request ID.",
        "Sequence",
    )
    xs = [160, 480, 800, 1120, 1440, 1760]
    parts = [
        ("sq-client", xs[0], "Client", "Mobile / web / partner", "external"),
        ("sq-gw", xs[1], "API Gateway", "JWT · rate limit · routing", "edge"),
        ("sq-rs", xs[2], "Request Service", "Admission API", "core"),
        ("sq-redis", xs[3], "Redis", "Lock and replay fast path", "data"),
        ("sq-db", xs[4], "PostgreSQL", "Request + Outbox", "data"),
        ("sq-pub", xs[5], "Publisher", "Asynchronous role", "messaging"),
    ]
    for args in parts:
        lifeline(d, *args)
    messages = [
        ("sqs-1", 0, 1, 310, "POST /api/v1/requests"),
        ("sqs-2", 1, 2, 385, "forward trusted identity"),
        ("sqs-3", 2, 3, 460, "acquire user-scoped key"),
        ("sqs-4", 3, 2, 525, "new / replay / conflict", True),
        ("sqs-5", 2, 4, 600, "BEGIN · insert request + outbox"),
        ("sqs-6", 4, 2, 665, "COMMIT", True),
        ("sqs-7", 2, 3, 730, "cache accepted response"),
        ("sqs-8", 2, 1, 795, "201 Created · Request ID", True),
        ("sqs-9", 1, 0, 860, "201 Created · PENDING", True),
        ("sqs-10", 5, 4, 930, "later: claim pending outbox"),
    ]
    for item in messages:
        edge_id, a, b, y, label, *rev = item
        message(d, edge_id, xs[a], xs[b], y, label, reverse=bool(rev and rev[0]))
    d.set_legend(("External caller", "external"), ("Edge security", "edge"),
                 ("Core admission", "core"), ("Durable state", "data"),
                 ("Async continuation", "messaging"))
    return finish(d)


def diagram_async_sequence() -> Diagram:
    d = Diagram(
        "05-sequence/asynchronous-processing-sequence",
        "Asynchronous Processing Sequence",
        "At-least-once delivery is made safe by Outbox publishing and an idempotent consumer.",
        "Sequence",
    )
    xs = [145, 415, 685, 955, 1225, 1495, 1765]
    parts = [
        ("as-pub", xs[0], "Outbox Publisher", "Claims batches", "messaging"),
        ("as-db", xs[1], "PostgreSQL", "Outbox + request", "data"),
        ("as-rabbit", xs[2], "RabbitMQ", "Queue + DLQ", "messaging"),
        ("as-worker", xs[3], "Worker", "Parallel consumer", "service"),
        ("as-processed", xs[4], "Processed Events", "Consumer idempotency", "data"),
        ("as-domain", xs[5], "Business Adapter", "Optional and replaceable", "external"),
        ("as-status", xs[6], "Request Record", "Final state", "data"),
    ]
    for args in parts:
        lifeline(d, *args)
    rows = [
        ("asq-1", 0, 1, 305, "claim PENDING batch"),
        ("asq-2", 1, 0, 365, "claimed events", True),
        ("asq-3", 0, 2, 425, "publish request.created"),
        ("asq-4", 2, 0, 485, "publisher confirm", True),
        ("asq-5", 0, 1, 545, "mark PUBLISHED"),
        ("asq-6", 2, 3, 605, "deliver event"),
        ("asq-7", 3, 4, 665, "INSERT event_id if absent"),
        ("asq-8", 4, 3, 725, "new / duplicate", True),
        ("asq-9", 3, 6, 785, "mark PROCESSING"),
        ("asq-10", 3, 5, 845, "optional domain execution"),
        ("asq-11", 3, 6, 905, "COMPLETED or FAILED"),
        ("asq-12", 3, 2, 955, "ACK · or reject to DLQ", True),
    ]
    for item in rows:
        edge_id, a, b, y, label, *rev = item
        message(d, edge_id, xs[a], xs[b], y, label, reverse=bool(rev and rev[0]))
    d.set_legend(("Outbox producer", "messaging"), ("Durable state", "data"),
                 ("Worker execution", "service"), ("Optional domain", "external"))
    return finish(d)


def diagram_state() -> Diagram:
    d = Diagram(
        "06-state/request-lifecycle-state",
        "Request Lifecycle State Machine",
        "Only four persisted request states exist in the implemented core domain.",
        "State",
    )
    n(d, "st-start", 95, 440, 160, 90, "Start", "Valid new request", "neutral", "state", 21, 14)
    n(d, "st-pending", 420, 390, 310, 170, "PENDING",
      "Request and Outbox event committed. Publishing may retry without changing this state.", "core", "state", 28, 16)
    n(d, "st-processing", 845, 390, 310, 170, "PROCESSING",
      "A Worker owns the event and starts idempotent processing.", "messaging", "state", 28, 16)
    n(d, "st-completed", 1310, 245, 360, 165, "COMPLETED",
      "Processing result persisted. Terminal success state.", "success", "state", 28, 16)
    n(d, "st-failed", 1310, 615, 360, 165, "FAILED",
      "Error persisted after rejection or retry exhaustion. Terminal failure state.", "failure", "state", 28, 16)
    n(d, "st-end-ok", 1740, 280, 120, 90, "End", "Success", "success", "state", 20, 13)
    n(d, "st-end-fail", 1740, 650, 120, 90, "End", "Failure", "failure", "state", 20, 13)
    e(d, "st-1", "st-start", "st-pending", "transaction committed")
    e(d, "st-2", "st-pending", "st-processing", "event consumed")
    e(d, "st-3", "st-processing", "st-completed", "processing succeeds", color="#16A34A")
    e(d, "st-4", "st-processing", "st-failed", "terminal failure", color="#E11D48")
    e(d, "st-5", "st-completed", "st-end-ok", "")
    e(d, "st-6", "st-failed", "st-end-fail", "")
    e(d, "st-dup", "st-processing", "st-processing", "duplicate event ignored", dashed=True, points=[(1000, 390), (1000, 300), (1180, 300), (1180, 475)])
    d.text("state-note", 440, 680, 650, 120,
           "Retry and redelivery are messaging behaviors. They do not create extra persisted request states.", 18, MUTED, True)
    d.set_legend(("Active state", "core"), ("Processing state", "messaging"),
                 ("Terminal success", "success"), ("Terminal failure", "failure"))
    return finish(d)


def diagram_class() -> Diagram:
    d = Diagram(
        "07-class/core-domain-class-diagram",
        "Core Domain and Reliability Components",
        "Implementation-level responsibilities across request admission, Outbox publishing and idempotent consumption.",
        "Class Model",
    )
    n(d, "cl-redis", 60, 225, 280, 240, "Redis Services",
      "Idempotency replay cache\nDistributed lock\nRequest cache\nDatabase fallback", "data",
      title_size=20, body_size=16, align="left")
    n(d, "cl-request", 400, 225, 300, 240, "RequestEntity",
      "id: UUID\nuserId: UUID\nstatus: RequestStatus\nrequestType · payload\nprocessingResult / error", "core",
      title_size=20, body_size=16, align="left")
    n(d, "cl-outbox", 740, 225, 300, 240, "OutboxEvent",
      "eventId · aggregateId\neventType · payload\nstatus · attempts\nclaimedAt · publishedAt", "messaging",
      title_size=20, body_size=16, align="left")
    n(d, "cl-processed", 1420, 225, 330, 240, "ProcessedEvent",
      "eventId: UUID\nrequestId: UUID\neventType · version\nprocessedAt", "data",
      title_size=20, body_size=16, align="left")

    n(d, "cl-controller", 60, 650, 280, 250, "RequestController",
      "createRequest()\ngetRequest()\nlistOwnRequests()", "edge",
      title_size=20, body_size=16, align="left")
    n(d, "cl-service", 400, 650, 300, 250, "RequestServiceImpl",
      "createRequest()\nfindByIdForCurrentUser()\nlistForCurrentUser()", "service",
      title_size=20, body_size=16, align="left")
    n(d, "cl-publisher", 740, 650, 300, 250, "OutboxEventProcessor",
      "claimBatch()\npublish()\nmarkPublished()\nmarkFailure()", "messaging",
      title_size=20, body_size=16, align="left")
    n(d, "cl-event", 1080, 650, 300, 250, "RequestCreatedEvent",
      "eventId · requestId\neventType · version\noccurredAt", "messaging",
      title_size=20, body_size=16, align="left")
    n(d, "cl-consumer", 1420, 650, 430, 250, "IdempotentRequestProcessingService",
      "tryRegisterEvent()\nprocessRequest()\nmarkEventAsProcessed()\nupdate final request state", "service",
      title_size=19, body_size=16, align="left")

    e(d, "cl-1", "cl-controller", "cl-service", "", points=[(340, 775), (400, 775)])
    e(d, "cl-2", "cl-service", "cl-request", "creates / reads",
      points=[(550, 650), (550, 465)])
    e(d, "cl-3", "cl-service", "cl-redis", "",
      points=[(400, 775), (370, 775), (370, 345), (340, 345)])
    e(d, "cl-4", "cl-service", "cl-outbox", "creates atomically",
      points=[(700, 735), (720, 735), (720, 530), (890, 530), (890, 465)])
    e(d, "cl-5", "cl-publisher", "cl-outbox", "claims / updates",
      points=[(890, 650), (890, 465)])
    e(d, "cl-6", "cl-publisher", "cl-event", "",
      points=[(1040, 775), (1080, 775)])
    e(d, "cl-7", "cl-event", "cl-consumer", "",
      points=[(1380, 775), (1420, 775)])
    e(d, "cl-8", "cl-consumer", "cl-processed", "registers once",
      points=[(1585, 650), (1585, 465)])
    d.text("cl-flow", 520, 935, 900, 34,
           "Admission  →  durable publication  →  idempotent consumption", 16, MUTED, False, "center")
    d.set_legend(("Domain entity", "core"), ("Reliability entity", "messaging"),
                 ("Application service", "service"), ("Edge/API", "edge"), ("Data support", "data"))
    return finish(d)


def diagram_c4_level1() -> Diagram:
    d = Diagram(
        "08-c4/c4-level1-system-context",
        "C4 Level 1 — System Context",
        "HLRMS is a general platform; every client application and business service is replaceable.",
        "C4 Architecture",
    )
    n(d, "c1-user", 80, 250, 300, 135, "Person · End User",
      "Uses a client application to submit and track a request", "external")
    n(d, "c1-admin", 80, 650, 300, 135, "Person · Operator",
      "Observes health and administers authorized requests", "ops")
    n(d, "c1-client", 480, 250, 350, 135, "Software System · Client Application",
      "Mobile, web, partner or public-service front end", "external")
    n(d, "c1-core", 950, 300, 520, 360, "Software System · HLRMS",
      "Authenticates callers\nAccepts requests quickly\nBuffers work durably\nProcesses asynchronously\nTracks state and reliability metrics",
      "core", title_size=28, body_size=18)
    n(d, "c1-domain", 1560, 250, 300, 145, "Software System · Business Service",
      "Optional domain execution owned outside the core platform", "external", dashed=True)
    n(d, "c1-ops", 1560, 650, 300, 145, "Software System · Observability",
      "Prometheus, Grafana and infrastructure exporters", "ops")
    e(d, "c1-1", "c1-user", "c1-client", "uses")
    e(d, "c1-2", "c1-client", "c1-core", "REST/JSON over HTTPS",
      points=[(830, 335), (900, 335), (900, 260), (1080, 260), (1080, 300)])
    e(d, "c1-3", "c1-core", "c1-client", "Request ID · status · result", dashed=True,
      points=[(1240, 300), (1240, 205), (700, 205), (700, 250)])
    e(d, "c1-4", "c1-core", "c1-domain", "optional domain command",
      points=[(1470, 385), (1510, 385), (1510, 210), (1710, 210), (1710, 250)])
    e(d, "c1-5", "c1-core", "c1-ops", "metrics and health",
      points=[(1470, 575), (1510, 575), (1510, 610), (1710, 610), (1710, 650)])
    e(d, "c1-6", "c1-admin", "c1-core", "admin API",
      points=[(380, 700), (720, 700), (720, 620), (950, 620)])
    e(d, "c1-7", "c1-admin", "c1-ops", "dashboards",
      points=[(380, 750), (1450, 750), (1450, 722), (1560, 722)])
    d.set_legend(("Person / external system", "external"), ("HLRMS system", "core"),
                 ("Operational system", "ops"))
    return finish(d)


def diagram_c4_level2() -> Diagram:
    d = Diagram(
        "08-c4/c4-level2-container",
        "C4 Level 2 — Container Architecture",
        "Stateless API containers scale horizontally while PostgreSQL, Redis and RabbitMQ provide shared state.",
        "C4 Architecture",
    )
    d.group("c2-edge", 55, 190, 430, 735, "ENTRY AND IDENTITY", "Synchronous access path", "edge", False)
    d.group("c2-core", 515, 190, 630, 735, "REQUEST CORE", "Durable admission and publication", "core", False)
    d.group("c2-async", 1175, 190, 410, 735, "ASYNC EXECUTION", "Buffered parallel processing", "messaging", False)
    d.group("c2-data", 1615, 190, 250, 735, "SHARED STATE", "Data and telemetry", "data", False)
    n(d, "c2-client", 95, 255, 350, 95, "Client Applications", "Replaceable callers · HTTPS", "external")
    n(d, "c2-gwlb", 95, 405, 350, 95, "API Gateway LB", "HAProxy :8088 · load balanced", "edge")
    n(d, "c2-gw", 95, 555, 350, 115, "API Gateway", "JWT · /auth/** · /requests/** · rate limit", "edge")
    n(d, "c2-auth", 95, 745, 350, 110, "Auth Service", "Register · login · refresh · RBAC", "security")
    n(d, "c2-rslb", 565, 255, 530, 95, "Request Service LB", "HAProxy round-robin · internal :8080", "edge")
    n(d, "c2-rs", 565, 405, 530, 125, "Request Service × 2 (baseline)",
      "Validation · ownership · idempotency · status · PostgreSQL / Redis", "core")
    n(d, "c2-pub", 565, 590, 530, 115, "Outbox Publisher × 1",
      "Claims PostgreSQL Outbox · publisher confirms · recovery", "messaging")
    n(d, "c2-demo", 565, 765, 530, 95, "Demo Business API (Replaceable)",
      "Outside HLRMS Core · optional execution target", "external", dashed=True)
    n(d, "c2-monitor", 1225, 290, 310, 95, "Prometheus + Grafana", "Metrics and dashboards", "ops")
    n(d, "c2-rabbit", 1225, 560, 310, 130, "RabbitMQ", "Exchange · processing queue · DLX · DLQ", "messaging", "queue")
    n(d, "c2-worker", 1225, 735, 310, 130, "Request Worker × N",
      "1–4 consumers per replica · idempotent processing · final-state persistence", "service")
    n(d, "c2-pg", 1650, 285, 180, 145, "PostgreSQL", "Auth DB\nRequests DB", "data", "database", 18, 14)
    n(d, "c2-redis", 1650, 505, 180, 130, "Redis", "Cache\nlocks\nrate limit", "data", "database", 18, 14)
    n(d, "c2-metrics", 1650, 725, 180, 130, "Exporters", "RabbitMQ\nPostgreSQL\nRedis", "ops", 18, 14)
    e(d, "c2-1", "c2-client", "c2-gwlb", "")
    e(d, "c2-2", "c2-gwlb", "c2-gw", "")
    e(d, "c2-3", "c2-gw", "c2-auth", "")
    e(d, "c2-4", "c2-gw", "c2-rslb", "",
      points=[(445, 612), (500, 612), (500, 302), (565, 302)])
    e(d, "c2-5", "c2-rslb", "c2-rs", "", points=[(830, 350), (830, 405)])
    e(d, "c2-9", "c2-pub", "c2-rabbit", "",
      points=[(1095, 647), (1160, 647), (1160, 625), (1225, 625)])
    e(d, "c2-10", "c2-rabbit", "c2-worker", "", points=[(1380, 690), (1380, 735)])
    e(d, "c2-12", "c2-worker", "c2-demo", "", dashed=True,
      points=[(1225, 800), (1160, 800), (1160, 812), (1095, 812)])
    e(d, "c2-13", "c2-monitor", "c2-metrics", "", dashed=True,
      points=[(1535, 338), (1580, 338), (1580, 790), (1650, 790)])
    d.set_legend(("Edge / access", "edge"), ("Core container", "core"),
                 ("Async container", "messaging"), ("Shared state", "data"),
                 ("Replaceable demo", "external"))
    return finish(d)


def diagram_c4_level3() -> Diagram:
    d = Diagram(
        "08-c4/c4-level3-request-processing-components",
        "C4 Level 3 — Request Processing Components",
        "Component responsibilities inside Request Service, Outbox Publisher and Request Worker.",
        "C4 Architecture",
    )
    d.group("l3-rs", 45, 180, 720, 760, "REQUEST SERVICE", "Synchronous admission components", "core", False)
    d.group("l3-pub", 795, 180, 420, 760, "OUTBOX PUBLISHER", "Asynchronous publication role", "messaging", False)
    d.group("l3-worker", 1245, 180, 630, 760, "REQUEST WORKER", "Idempotent execution components", "service", False)
    n(d, "l3-controller", 95, 250, 615, 100, "1 · RequestController",
      "REST contract · validation · caller identity", "edge")
    n(d, "l3-service", 95, 400, 300, 120, "2 · RequestServiceImpl",
      "Ownership · orchestration · replay decisions", "service")
    n(d, "l3-idem", 430, 400, 280, 120, "3 · Redis Idempotency",
      "Lock · fingerprint · cache · DB fallback", "data")
    n(d, "l3-tx", 95, 590, 300, 130, "4 · Creation Transaction",
      "Atomic request + Outbox commit", "core")
    n(d, "l3-repos", 430, 590, 280, 130, "5 · Repositories + Cache",
      "Request · Outbox · read-through cache", "data")

    n(d, "l3-processor", 850, 280, 310, 140, "6 · OutboxEventProcessor",
      "Schedule · claim batch · retry policy", "messaging")
    n(d, "l3-publisher", 850, 560, 310, 140, "7 · RequestEventPublisher",
      "RabbitTemplate · confirms · returns", "messaging")

    n(d, "l3-consumer", 1285, 250, 275, 115, "8 · RequestEventConsumer",
      "Validate the incoming event", "messaging", title_size=17, body_size=15)
    n(d, "l3-idconsumer", 1590, 250, 230, 115, "9 · Idempotent Consumer",
      "Register event_id once", "service", title_size=20)
    n(d, "l3-process", 1300, 500, 250, 125, "10 · Processing Service",
      "Run generic or optional domain work", "service", title_size=20)
    n(d, "l3-status", 1590, 500, 230, 125, "11 · Status Transaction",
      "PROCESSING · COMPLETED · FAILED", "data", title_size=20)
    n(d, "l3-recover", 1445, 750, 260, 115, "Failure Recoverer",
      "Mark FAILED and reject to DLQ", "failure")

    e(d, "l3-1", "l3-controller", "l3-service", "",
      points=[(405, 350), (405, 375), (245, 375), (245, 400)])
    e(d, "l3-2", "l3-service", "l3-idem", "", points=[(395, 460), (430, 460)])
    e(d, "l3-3", "l3-service", "l3-tx", "", points=[(245, 520), (245, 590)])
    e(d, "l3-4", "l3-tx", "l3-repos", "", points=[(395, 655), (430, 655)])
    e(d, "l3-5", "l3-repos", "l3-processor", "", dashed=True,
      points=[(710, 655), (760, 655), (760, 350), (850, 350)])
    e(d, "l3-6", "l3-processor", "l3-publisher", "", points=[(1005, 420), (1005, 560)])
    e(d, "l3-7", "l3-publisher", "l3-consumer", "", dashed=True,
      points=[(1160, 630), (1225, 630), (1225, 308), (1285, 308)])
    e(d, "l3-8", "l3-consumer", "l3-idconsumer", "", points=[(1560, 308), (1590, 308)])
    e(d, "l3-9", "l3-idconsumer", "l3-process", "",
      points=[(1705, 365), (1705, 435), (1425, 435), (1425, 500)])
    e(d, "l3-10", "l3-process", "l3-status", "", points=[(1550, 562), (1590, 562)])
    e(d, "l3-11", "l3-process", "l3-recover", "", color="#E11D48",
      points=[(1425, 625), (1425, 705), (1575, 705), (1575, 750)])
    d.set_legend(("REST component", "edge"), ("Core component", "core"),
                 ("Messaging component", "messaging"), ("Data component", "data"),
                 ("Failure component", "failure"))
    return finish(d)


def diagram_deployment_overview() -> Diagram:
    d = Diagram(
        "09-deployment/deployment-overview",
        "Deployment Overview",
        "Current local deployment with an optional public tunnel for the functional demo only.",
        "Deployment",
    )
    n(d, "dep-users", 65, 245, 290, 110, "Demo Users", "Android, web or browser", "external")
    n(d, "dep-ngrok", 65, 530, 290, 120, "ngrok Tunnel (Optional)",
      "Stable public HTTPS endpoint for functional demo", "external", dashed=True)
    d.group("dep-host", 430, 175, 1435, 770, "SYSTEM UNDER TEST HOST",
            "Windows + WSL2 + Docker Compose", "core", False)
    d.group("dep-edge", 480, 245, 360, 610, "EDGE", "Published port :8088", "edge", False)
    d.group("dep-app", 870, 245, 500, 610, "APPLICATION", "Spring Boot services", "service", False)
    d.group("dep-state", 1400, 245, 410, 610, "STATE + OPERATIONS", "Shared infrastructure", "data", False)
    n(d, "dep-gwlb", 530, 320, 260, 100, "Gateway LB", "HAProxy :8088", "edge")
    n(d, "dep-gw", 530, 500, 260, 110, "API Gateway", "Internal :8088", "edge")
    n(d, "dep-rslb", 530, 690, 260, 100, "Request LB", "HAProxy :8080", "edge")
    n(d, "dep-auth", 925, 305, 390, 95, "Auth Service × 1", ":8081", "security")
    n(d, "dep-rs", 925, 455, 390, 105, "Request Service × 2", ":8080 per replica", "core")
    n(d, "dep-pub", 925, 615, 390, 95, "Outbox Publisher × 1", "Dedicated publisher role", "messaging")
    n(d, "dep-worker", 925, 765, 390, 95, "Request Worker × N", ":8082 · scaling experiment 1/2/4/8", "service")
    n(d, "dep-pg", 1450, 325, 310, 100, "PostgreSQL 17",
      "Auth and Requests databases", "data", "database")
    n(d, "dep-redis", 1450, 465, 310, 95, "Redis 8",
      "Cache · locks · rate limiting", "data", "database")
    n(d, "dep-rabbit", 1450, 600, 310, 100, "RabbitMQ 4.x",
      ":5672 · management :15672", "messaging", "queue")
    n(d, "dep-obs", 1450, 740, 310, 100, "Prometheus + Grafana",
      ":9090 · :3000 · exporters", "ops")
    e(d, "dep-1", "dep-users", "dep-ngrok", "HTTPS", dashed=True)
    e(d, "dep-2", "dep-ngrok", "dep-gwlb", "ngrok → localhost:8088", dashed=True,
      points=[(355, 590), (400, 590), (400, 395), (530, 395)])
    e(d, "dep-3", "dep-users", "dep-gwlb", "LAN / local",
      points=[(355, 300), (450, 300), (450, 345), (530, 345)])
    e(d, "dep-4", "dep-gwlb", "dep-gw", "")
    e(d, "dep-5", "dep-gw", "dep-auth", "auth routes")
    e(d, "dep-6", "dep-gw", "dep-rslb", "request routes")
    e(d, "dep-7", "dep-rslb", "dep-rs", "round-robin")
    e(d, "dep-8", "dep-rs", "dep-pg", "",
      points=[(1315, 508), (1390, 508), (1390, 375), (1450, 375)])
    e(d, "dep-9", "dep-pub", "dep-rabbit", "",
      points=[(1315, 662), (1400, 662), (1400, 650), (1450, 650)])
    e(d, "dep-10", "dep-rabbit", "dep-worker", "",
      points=[(1450, 650), (1380, 650), (1380, 812), (1315, 812)])
    e(d, "dep-11", "dep-rs", "dep-redis", "",
      points=[(1315, 520), (1400, 520), (1400, 512), (1450, 512)])
    e(d, "dep-12", "dep-obs", "dep-rs", "", dashed=True,
      points=[(1450, 790), (1410, 790), (1410, 545), (1315, 545)])
    d.set_legend(("External access", "external"), ("Edge tier", "edge"),
                 ("Application tier", "service"), ("Data tier", "data"),
                 ("Operations", "ops"))
    return finish(d)


def diagram_compose_topology() -> Diagram:
    d = Diagram(
        "09-deployment/docker-compose-topology",
        "Docker Compose Topology",
        "Service-to-service traffic remains on the internal Docker network; only selected ports are published.",
        "Deployment",
    )
    d.group("dc-public", 45, 185, 370, 770, "PUBLISHED PORTS", "Host-facing endpoints", "external", False)
    d.group("dc-net", 445, 185, 1425, 770, "DOCKER COMPOSE NETWORK", "Internal service discovery by DNS name", "core", False)
    n(d, "dc-8088", 95, 270, 270, 95, ":8088", "API Gateway LB", "edge")
    n(d, "dc-3000", 95, 435, 270, 95, ":3000", "Grafana", "ops")
    n(d, "dc-9090", 95, 600, 270, 95, ":9090", "Prometheus", "ops")
    n(d, "dc-mgmt", 95, 765, 270, 105, ":15672", "RabbitMQ management", "messaging")
    n(d, "dc-gwlb", 500, 255, 250, 100, "api-gateway-lb", "HAProxy", "edge")
    n(d, "dc-gw", 825, 255, 250, 100, "api-gateway", "Spring WebFlux", "edge")
    n(d, "dc-auth", 1150, 255, 250, 100, "auth-service", "Spring Boot", "security")
    n(d, "dc-rslb", 825, 450, 250, 100, "request-service-lb", "HAProxy", "edge")
    n(d, "dc-rs", 1150, 450, 250, 100, "request-service × 2", "Spring Boot", "core")
    n(d, "dc-pub", 500, 645, 250, 100, "outbox-publisher", "Publisher role", "messaging")
    n(d, "dc-rabbit", 825, 645, 250, 100, "rabbitmq", "Broker", "messaging", "queue")
    n(d, "dc-worker", 1150, 645, 250, 100, "request-worker × N", "Consumers", "service")
    n(d, "dc-pg", 1490, 255, 300, 100, "postgres", "Two logical databases", "data", "database")
    n(d, "dc-redis", 1490, 450, 300, 100, "redis", "DB 0 core · DB 1 gateway", "data", "database")
    n(d, "dc-obs", 1490, 645, 300, 145, "prometheus + grafana",
      "postgres-exporter\nredis-exporter\nRabbitMQ metrics", "ops")
    e(d, "dc-1", "dc-8088", "dc-gwlb", "published")
    e(d, "dc-2", "dc-gwlb", "dc-gw", "")
    e(d, "dc-3", "dc-gw", "dc-auth", "/auth")
    e(d, "dc-4", "dc-gw", "dc-rslb", "/requests")
    e(d, "dc-5", "dc-rslb", "dc-rs", "")
    e(d, "dc-6", "dc-rs", "dc-pg", "",
      points=[(1400, 480), (1440, 480), (1440, 305), (1490, 305)])
    e(d, "dc-7", "dc-rs", "dc-redis", "", points=[(1400, 500), (1490, 500)])
    e(d, "dc-8", "dc-pub", "dc-rabbit", "", points=[(750, 695), (825, 695)])
    e(d, "dc-9", "dc-rabbit", "dc-worker", "", points=[(1075, 695), (1150, 695)])
    e(d, "dc-10", "dc-worker", "dc-pg", "",
      points=[(1400, 680), (1460, 680), (1460, 335), (1490, 335)])
    d.text("dc-published-note", 70, 895, 320, 38,
           "Ports map to the corresponding named containers.", 15, MUTED, True, "center")
    d.set_legend(("Published endpoint", "external"), ("Edge container", "edge"),
                 ("Core container", "core"), ("Message container", "messaging"),
                 ("Data / ops", "data"))
    return finish(d)


def diagram_two_host_topology() -> Diagram:
    d = Diagram(
        "09-deployment/two-host-performance-topology",
        "Two-Host Performance Validation Topology",
        "The load generator is isolated from the System Under Test to prevent local CPU contention from corrupting results.",
        "Deployment",
    )
    d.group("th-load", 55, 205, 510, 700, "HOST A · LOAD GENERATOR",
            "Laptop running k6 only", "external", False)
    d.group("th-sut", 720, 170, 1145, 780, "HOST B · SYSTEM UNDER TEST",
            "Docker / HLRMS and monitoring", "core", False)
    n(d, "th-k6", 125, 300, 370, 150, "k6 Runner",
      "ramping-arrival-rate\n350 → 500 → 750 → 1000 RPS", "external", title_size=25, body_size=18)
    n(d, "th-evidence", 125, 610, 370, 155, "Local Evidence Files",
      "summary JSON · raw metrics · command · TEST_RUN_ID", "ops", title_size=22, body_size=17)
    n(d, "th-lan", 585, 315, 120, 120, "LAN", "Direct private IP", "edge", "pill", 21, 15)
    n(d, "th-lb", 790, 315, 300, 120, "Gateway Load Balancer", "Host B private-IP:8088", "edge")
    n(d, "th-core", 1190, 300, 570, 150, "HLRMS Runtime",
      "Gateway · 2 Request Services · Publisher · RabbitMQ · Workers · PostgreSQL · Redis", "core", title_size=24, body_size=17)
    n(d, "th-prom", 790, 555, 300, 130, "Prometheus", "Collects service and infrastructure metrics", "ops")
    n(d, "th-graf", 1190, 555, 290, 130, "Grafana", "Live throughput · latency · queue · errors", "ops")
    n(d, "th-dbcheck", 1540, 555, 270, 130, "Integrity Check", "accepted = outbox = published = processed", "data", title_size=21, body_size=15)
    n(d, "th-review", 1040, 790, 500, 100, "Committee / Observer Screen",
      "k6 terminal + Grafana live + post-run reconciliation", "external")
    e(d, "th-1", "th-k6", "th-lan", "HTTP over LAN",
      points=[(495, 375), (585, 375)])
    e(d, "th-2", "th-lan", "th-lb", "Host B IP",
      points=[(705, 375), (790, 375)])
    e(d, "th-3", "th-lb", "th-core", "load traffic",
      points=[(1090, 375), (1190, 375)])
    e(d, "th-4", "th-core", "th-prom", "metrics", dashed=True,
      points=[(1340, 450), (1340, 500), (940, 500), (940, 555)])
    e(d, "th-5", "th-prom", "th-graf", "PromQL",
      points=[(1090, 620), (1190, 620)])
    e(d, "th-6", "th-core", "th-dbcheck", "post-run state",
      points=[(1475, 450), (1475, 510), (1675, 510), (1675, 555)])
    e(d, "th-7", "th-k6", "th-evidence", "export",
      points=[(310, 450), (310, 610)])
    e(d, "th-8", "th-graf", "th-review", "live dashboard",
      points=[(1335, 685), (1335, 735), (1180, 735), (1180, 790)])
    e(d, "th-9", "th-dbcheck", "th-review", "final proof",
      points=[(1675, 685), (1675, 750), (1400, 750), (1400, 790)])
    d.text("th-warning", 760, 905, 1050, 45,
           "Do not route the 1000 RPS test through ngrok; the tunnel would become an uncontrolled external bottleneck.", 17, "#9A3412", True)
    d.set_legend(("Load host", "external"), ("Network entry", "edge"),
                 ("System Under Test", "core"), ("Observability", "ops"),
                 ("Integrity evidence", "data"))
    return finish(d)


def diagram_auth_erd() -> Diagram:
    d = Diagram(
        "10-database/authentication-database-erd",
        "Authentication Database ERD",
        "PostgreSQL database: hlrms_auth · Flyway-managed schema.",
        "Data Model",
    )
    n(d, "er-users", 95, 230, 440, 560, "users",
      "PK  id · UUID\nUQ  email · VARCHAR\npassword_hash · VARCHAR\nfirst_name · VARCHAR\nlast_name · VARCHAR\naccount_status · VARCHAR\ncreated_at · TIMESTAMPTZ\nupdated_at · TIMESTAMPTZ",
      "data", "card", 27, 18, "left")
    n(d, "er-roles", 740, 230, 440, 330, "roles",
      "PK  id · UUID\nUQ  name · VARCHAR\ncreated_at · TIMESTAMPTZ",
      "security", "card", 27, 18, "left")
    n(d, "er-userroles", 740, 650, 440, 250, "user_roles",
      "PK/FK  user_id → users.id\nPK/FK  role_id → roles.id",
      "security", "card", 27, 18, "left")
    n(d, "er-refresh", 1385, 230, 440, 560, "refresh_tokens",
      "PK  id · UUID\nFK  user_id → users.id\nUQ  token_hash · VARCHAR\nexpires_at · TIMESTAMPTZ\nrevoked_at · TIMESTAMPTZ\ncreated_at · TIMESTAMPTZ",
      "data", "card", 27, 18, "left")
    e(d, "era-1", "er-users", "er-userroles", "1:N · user_roles.user_id",
      points=[(535, 715), (740, 715)])
    e(d, "era-2", "er-roles", "er-userroles", "1:N · user_roles.role_id",
      points=[(960, 560), (960, 650)], label_dx=88)
    e(d, "era-3", "er-users", "er-refresh", "1:N · refresh_tokens.user_id",
      points=[(315, 230), (315, 180), (1605, 180), (1605, 230)])
    d.text("era-note", 95, 850, 440, 80,
           "Passwords are BCrypt hashes. Refresh tokens are stored as hashes and rotated on refresh.", 17, MUTED, True)
    d.set_legend(("Identity data", "data"), ("Authorization data", "security"))
    return finish(d)


def diagram_database_landscape() -> Diagram:
    d = Diagram(
        "10-database/database-landscape",
        "Database Landscape and Ownership",
        "HLRMS owns authentication and request reliability data; client-domain data remains outside the core platform.",
        "Data Model",
    )
    d.group("dbl-core", 60, 190, 1250, 730, "HLRMS-OWNED DATA", "PostgreSQL 17 · Flyway migrations", "core", False)
    d.group("dbl-ext", 1350, 190, 510, 730, "CLIENT / BUSINESS-OWNED DATA", "Replaceable and outside HLRMS Core", "external", True)
    n(d, "dbl-auth", 130, 300, 500, 420, "hlrms_auth",
      "users\nroles\nuser_roles\nrefresh_tokens", "security", "database", 29, 20)
    n(d, "dbl-requests", 750, 300, 500, 420, "hlrms_requests",
      "requests\noutbox_events\nprocessed_events", "data", "database", 29, 20)
    n(d, "dbl-domain", 1425, 310, 360, 400, "External Domain Store",
      "Profiles\nBusiness records\nDomain transactions\nAny client-specific schema", "external", "database", 27, 19, dashed=True)
    e(d, "dbl-1", "dbl-auth", "dbl-requests", "user_id logical reference", dashed=True,
      points=[(380, 300), (380, 275), (1000, 275), (1000, 300)], label_dy=30)
    e(d, "dbl-2", "dbl-requests", "dbl-domain", "requestId / userId integration identifiers", dashed=True,
      points=[(1000, 300), (1000, 275), (1605, 275), (1605, 310)], label_dy=30)
    d.text("dbl-rule", 160, 795, 1080, 75,
           "No foreign key crosses database or system boundaries. Integration uses API contracts and stable identifiers.", 18, MUTED, True, "center")
    d.text("dbl-ext-note", 1425, 765, 360, 100,
           "The demonstration application's database is intentionally excluded from the HLRMS core schema.", 17, "#78350F", True, "center")
    d.set_legend(("Core-owned database", "data"), ("Identity database", "security"),
                 ("External domain database", "external"), ("Logical reference", "neutral"))
    return finish(d)


def diagram_requests_erd() -> Diagram:
    d = Diagram(
        "10-database/requests-database-erd",
        "Request Processing Database ERD",
        "PostgreSQL database: hlrms_requests · reliability state is persisted beside each request.",
        "Data Model",
    )
    n(d, "err-requests", 65, 220, 500, 650, "requests",
      "PK  id · UUID\nIDX user_id · UUID\nrequest_type · VARCHAR\npayload · TEXT\nstatus · VARCHAR\nprocessing_result · TEXT\nprocessing_error · TEXT\ncreated_at · TIMESTAMPTZ\nupdated_at · TIMESTAMPTZ",
      "core", "card", 27, 18, "left")
    n(d, "err-outbox", 710, 220, 500, 650, "outbox_events",
      "PK  id · UUID\nIDX aggregate_id → requests.id\nevent_type · VARCHAR\npayload · TEXT\nstatus · VARCHAR\nattempt_count · INTEGER\nclaimed_at / claimed_by\npublished_at · TIMESTAMPTZ\nlast_error · TEXT",
      "messaging", "card", 27, 18, "left")
    n(d, "err-processed", 1355, 220, 500, 650, "processed_events",
      "PK  event_id · UUID\nIDX request_id → requests.id\nevent_type · VARCHAR\nevent_version · INTEGER\noccurred_at · TIMESTAMPTZ\nprocessed_at · TIMESTAMPTZ",
      "data", "card", 27, 18, "left")
    e(d, "err-1", "err-requests", "err-outbox", "1:N · aggregate_id",
      points=[(565, 545), (710, 545)])
    e(d, "err-2", "err-requests", "err-processed", "1:N · request_id",
      points=[(315, 220), (315, 180), (1605, 180), (1605, 220)])
    d.text("err-note", 110, 905, 1690, 55,
           "OutboxEvent and ProcessedEvent use unique event identifiers to provide producer and consumer idempotency.", 18, MUTED, True, "center")
    d.set_legend(("Request aggregate", "core"), ("Producer reliability", "messaging"),
                 ("Consumer reliability", "data"))
    return finish(d)


def diagram_rabbit_topology() -> Diagram:
    d = Diagram(
        "11-rabbitmq/rabbitmq-topology",
        "RabbitMQ Topology",
        "Durable direct exchanges isolate normal delivery from terminal failures.",
        "Messaging",
    )
    n(d, "rmq-pub", 80, 328, 300, 135, "Outbox Publisher",
      "Publishes RequestCreatedEvent with confirms", "service")
    n(d, "rmq-ex", 500, 320, 330, 150, "Direct Exchange",
      "hlrms.request.exchange\nrouting key: request.created", "messaging")
    n(d, "rmq-q", 970, 320, 370, 150, "Processing Queue",
      "hlrms.request.processing.queue\ndurable · prefetch 10", "messaging", "queue")
    n(d, "rmq-workers", 1510, 285, 330, 210, "Request Workers × N",
      "Simple listener\nconcurrency 1\nmax concurrency 4 per replica\nauto ACK on success", "service")
    n(d, "rmq-dlx", 500, 690, 330, 140, "Dead-Letter Exchange",
      "hlrms.request.dlx\nrouting key: request.failed", "failure")
    n(d, "rmq-dlq", 970, 690, 370, 140, "Dead-Letter Queue",
      "hlrms.request.processing.dlq\ndurable terminal-failure buffer", "failure", "queue")
    n(d, "rmq-ops", 1510, 675, 330, 155, "Operations",
      "Queue depth · consumers · delivery · redelivery · DLQ", "ops")
    e(d, "rmq-1", "rmq-pub", "rmq-ex", "AMQP publish")
    e(d, "rmq-2", "rmq-ex", "rmq-q", "")
    e(d, "rmq-3", "rmq-q", "rmq-workers", "deliver")
    e(d, "rmq-4", "rmq-workers", "rmq-q", "retry / redelivery", dashed=True,
      points=[(1675, 495), (1675, 575), (1155, 575), (1155, 470)])
    e(d, "rmq-5", "rmq-q", "rmq-dlx", "reject · no requeue", color="#E11D48",
      points=[(1155, 470), (1155, 600), (665, 600), (665, 690)])
    e(d, "rmq-6", "rmq-dlx", "rmq-dlq", "", color="#E11D48")
    e(d, "rmq-7", "rmq-ops", "rmq-q", "", dashed=True)
    e(d, "rmq-8", "rmq-ops", "rmq-dlq", "", dashed=True)
    d.set_legend(("Publisher / consumer", "service"), ("Normal route", "messaging"),
                 ("Dead-letter route", "failure"), ("Operations", "ops"))
    return finish(d)


def diagram_message_lifecycle() -> Diagram:
    d = Diagram(
        "11-rabbitmq/message-lifecycle",
        "Reliable Message Lifecycle",
        "Every accepted request progresses through durable producer and consumer checkpoints.",
        "Messaging",
    )
    stages = [
        ("ml-1", 60, "1 · Commit", "Request + Outbox in one DB transaction", "data"),
        ("ml-2", 330, "2 · Claim", "Publisher atomically claims a pending batch", "messaging"),
        ("ml-3", 600, "3 · Publish", "Direct exchange + publisher confirm", "messaging"),
        ("ml-4", 870, "4 · Buffer", "Durable processing queue absorbs spikes", "messaging"),
        ("ml-5", 1140, "5 · Register", "Consumer inserts event_id if absent", "data"),
        ("ml-6", 1410, "6 · Execute", "Worker updates PROCESSING then executes", "service"),
        ("ml-7", 1680, "7 · Finalize", "COMPLETED / FAILED + ACK", "success"),
    ]
    for node_id, x, title, subtitle, kind in stages:
        n(d, node_id, x, 335, 205, 190, title, subtitle, kind, title_size=22, body_size=15)
    for idx in range(1, 7):
        e(d, f"ml-e-{idx}", f"ml-{idx}", f"ml-{idx+1}", "")
    n(d, "ml-retry", 600, 690, 310, 130, "Transient Publish Failure",
      "Outbox remains retryable; claim is released or retried", "external")
    n(d, "ml-redeliver", 1040, 690, 310, 130, "Consumer Failure",
      "Listener retry with exponential backoff", "external")
    n(d, "ml-dlq", 1480, 690, 310, 130, "Retry Exhausted",
      "Request marked FAILED and message routed to DLQ", "failure")
    e(d, "ml-f-1", "ml-3", "ml-retry", "no confirm / return", color="#D97706")
    e(d, "ml-f-2", "ml-retry", "ml-2", "retry later", dashed=True)
    e(d, "ml-f-3", "ml-6", "ml-redeliver", "exception", color="#D97706")
    e(d, "ml-f-4", "ml-redeliver", "ml-5", "redeliver", dashed=True)
    e(d, "ml-f-5", "ml-redeliver", "ml-dlq", "max attempts", color="#E11D48")
    d.text("ml-guarantee", 130, 895, 1660, 55,
           "Delivery model: at least once. Correctness model: no duplicate business effect for the same event_id.",
           20, INK, True, "center")
    d.set_legend(("Durable checkpoint", "data"), ("Messaging stage", "messaging"),
                 ("Worker stage", "service"), ("Recovery path", "external"), ("DLQ", "failure"))
    return finish(d)


def diagram_monitoring_architecture() -> Diagram:
    d = Diagram(
        "12-monitoring/monitoring-architecture",
        "Monitoring Architecture",
        "Application and infrastructure metrics converge in Prometheus and are presented through one Grafana overview.",
        "Observability",
    )
    d.group("mon-app", 55, 190, 600, 710, "APPLICATION METRICS", "Spring Boot Actuator + Micrometer", "service", False)
    d.group("mon-infra", 690, 190, 520, 710, "INFRASTRUCTURE METRICS", "Native endpoints and exporters", "data", False)
    d.group("mon-ops", 1245, 190, 620, 710, "COLLECTION AND VISUALIZATION", "Prometheus + Grafana", "ops", False)
    n(d, "mon-app-sources", 105, 295, 500, 420, "Application Metric Sources",
      "API Gateway · Request Service\nAuth Service · Request Worker\nHealth and readiness probes\nActuator + Micrometer\n/actuator/prometheus",
      "service", title_size=26, body_size=20, align="left")
    n(d, "mon-infra-sources", 745, 295, 410, 420, "Infrastructure Sources",
      "RabbitMQ metrics\nPostgreSQL exporter\nRedis exporter\nJVM / process metrics\nNative endpoints and exporters",
      "data", title_size=26, body_size=20, align="left")
    n(d, "mon-prom", 1320, 290, 470, 180, "Prometheus",
      "15-second scrape interval\nPromQL rates and histogram quantiles", "ops", title_size=28, body_size=19)
    n(d, "mon-graf", 1320, 600, 470, 180, "Grafana",
      "Dashboard: HLRMS System Overview\nHealth · throughput · latency · worker\nbroker · DB · Redis · resilience",
      "ops", title_size=27, body_size=17)
    e(d, "mon-e-1", "mon-app-sources", "mon-prom", "", dashed=True,
      points=[(605, 650), (640, 650), (640, 840), (1270, 840), (1270, 380), (1320, 380)])
    e(d, "mon-e-2", "mon-infra-sources", "mon-prom", "", dashed=True,
      points=[(1155, 390), (1320, 390)])
    e(d, "mon-e-3", "mon-prom", "mon-graf", "Prometheus datasource",
      points=[(1555, 470), (1555, 600)])
    d.set_legend(("Application metric source", "service"), ("Infrastructure source", "data"),
                 ("Messaging source", "messaging"), ("Observability stack", "ops"))
    return finish(d)


def diagram_metrics_alerts() -> Diagram:
    d = Diagram(
        "12-monitoring/metrics-and-alerts",
        "Metrics and Operational Signals",
        "The dashboard correlates client-visible performance with queue, worker and storage behavior.",
        "Observability",
    )
    cards = [
        ("ma-http", 70, 210, "HTTP EDGE", "Requests/sec\nP95 latency\n5xx rate\n429 rate", "edge"),
        ("ma-admit", 430, 210, "REQUEST ADMISSION", "Requests created\nIdempotency replays\nCreation failures\nHikariCP pool", "core"),
        ("ma-worker", 790, 210, "WORKER", "Completion rate\nFailed total\nProcessing avg/P95\nActive consumers", "service"),
        ("ma-rabbit", 1150, 210, "RABBITMQ", "Ready messages\nUnacked messages\nDelivery/redelivery\nDLQ depth", "messaging"),
        ("ma-data", 1510, 210, "DATA STORES", "PG connections/xacts\nCache hit ratio\nRedis memory\nEvictions", "data"),
    ]
    for node_id, x, y, title, subtitle, kind in cards:
        n(d, node_id, x, y, 310, 310, title, subtitle, kind, title_size=23, body_size=19, align="left")
    n(d, "ma-good", 90, 650, 500, 190, "Healthy Run",
      "Throughput tracks target\nError rate remains within threshold\nQueue returns to zero\nNo new DLQ messages", "success", title_size=25, body_size=18, align="left")
    n(d, "ma-sat", 710, 650, 500, 190, "Saturation Signal",
      "Dropped iterations increase\nP95/P99 rises\nCPU / connection pool reaches limit\nQueue drain time increases", "external", title_size=25, body_size=18, align="left")
    n(d, "ma-fail", 1330, 650, 500, 190, "Reliability Alert",
      "Outbox pending grows permanently\nDLQ delta > 0\nWorker completion stops\nReadiness goes DOWN", "failure", title_size=25, body_size=18, align="left")
    n(d, "ma-correlate", 620, 555, 680, 60,
      "Correlate every panel in the same TEST_RUN_ID time window", "", "ops", shape="pill", title_size=19)
    d.text("ma-note", 250, 895, 1420, 48,
           "Evidence must be captured during a named test run and evaluated together with the k6 summary and database reconciliation.", 18, MUTED, True, "center")
    d.set_legend(("Client-facing", "edge"), ("Core admission", "core"),
                 ("Async processing", "service"), ("Broker", "messaging"),
                 ("Storage", "data"))
    return finish(d)


def diagram_security_boundaries() -> Diagram:
    d = Diagram(
        "13-security/security-trust-boundaries",
        "Security Trust Boundaries",
        "Only the Gateway is trusted to assert identity headers to internal services.",
        "Security",
    )
    d.group("sec-untrusted", 45, 190, 420, 735, "UNTRUSTED ZONE", "Internet or client network", "external", False)
    d.group("sec-edge", 500, 190, 420, 735, "EDGE TRUST BOUNDARY", "Public API entry", "edge", False)
    d.group("sec-app", 955, 190, 530, 735, "INTERNAL SERVICE NETWORK", "No direct public access", "security", False)
    d.group("sec-data", 1520, 190, 345, 735, "DATA BOUNDARY", "Authenticated service access", "data", False)
    n(d, "sec-client", 100, 300, 310, 120, "Client Application", "Bearer token · payload · idempotency key", "external")
    n(d, "sec-attacker", 100, 625, 310, 130, "Untrusted Input", "Forged X-User-* headers · replay · burst traffic", "failure")
    n(d, "sec-lb", 555, 285, 310, 105, "Gateway Load Balancer", "Single published port", "edge")
    n(d, "sec-gw", 555, 485, 310, 180, "API Gateway",
      "Validate JWT\nStrip untrusted identity headers\nAdd trusted identity\nRate limit · CORS · timeouts", "security", title_size=23, body_size=17)
    n(d, "sec-auth", 1010, 285, 420, 115, "Auth Service", "BCrypt · JWT access token · hashed refresh token rotation", "security")
    n(d, "sec-rs", 1010, 485, 420, 140, "Request Service", "Trusted headers · ownership isolation · USER / ADMIN RBAC", "core")
    n(d, "sec-worker", 1010, 725, 420, 115, "Worker + Publisher", "Internal AMQP/JDBC · shared secrets for optional adapter", "service")
    n(d, "sec-pg", 1570, 285, 245, 125, "PostgreSQL", "Separate logical databases", "data", "database")
    n(d, "sec-redis", 1570, 505, 245, 115, "Redis", "Password · separate DB indexes", "data", "database")
    n(d, "sec-rabbit", 1570, 720, 245, 115, "RabbitMQ", "Credentials · durable queues", "messaging", "queue")
    e(d, "sec-1", "sec-client", "sec-lb", "HTTPS")
    e(d, "sec-2", "sec-attacker", "sec-gw", "hostile input", color="#E11D48")
    e(d, "sec-3", "sec-lb", "sec-gw", "")
    e(d, "sec-4", "sec-gw", "sec-auth", "auth routes")
    e(d, "sec-5", "sec-gw", "sec-rs", "trusted X-User-* headers")
    e(d, "sec-6", "sec-auth", "sec-pg", "JDBC")
    e(d, "sec-7", "sec-rs", "sec-pg", "JDBC")
    e(d, "sec-8", "sec-rs", "sec-redis", "RESP")
    e(d, "sec-9", "sec-worker", "sec-rabbit", "AMQP")
    d.set_legend(("Untrusted", "external"), ("Rejected threat", "failure"),
                 ("Gateway control", "security"), ("Internal service", "service"),
                 ("Protected data", "data"))
    return finish(d)


def diagram_client_integration() -> Diagram:
    d = Diagram(
        "14-client-integration/client-integration-architecture",
        "Generic Client Integration Architecture",
        "The supplied Android application is one demonstration client; any mobile, web or service client can use the same contract.",
        "Integration",
    )
    d.group("ci-clients", 50, 190, 480, 730, "REPLACEABLE CLIENTS", "Outside HLRMS Core", "external", False)
    d.group("ci-contract", 565, 190, 430, 730, "STABLE API CONTRACT", "REST/JSON + JWT", "edge", False)
    d.group("ci-core", 1030, 190, 835, 730, "HLRMS CORE", "Application-agnostic processing", "core", False)
    n(d, "ci-clients-list", 110, 300, 360, 460, "Client Systems",
      "Mobile application\nWeb application\nPartner / backend system\nPublic-service portal\nAny REST-capable client\n\nThe supplied Android app is one demo client.",
      "external", title_size=27, body_size=20, align="left")
    n(d, "ci-auth", 620, 260, 320, 105, "Authentication API", "Register · login · refresh", "security")
    n(d, "ci-submit", 620, 440, 320, 125, "Request API", "Submit with Idempotency-Key\nreceive Request ID", "core")
    n(d, "ci-status", 620, 650, 320, 105, "Status API", "Poll by Request ID · list history", "service")
    n(d, "ci-gateway", 1090, 330, 300, 170, "API Gateway",
      "Routing · JWT · rate limit · correlation", "edge")
    n(d, "ci-auth-service", 1480, 260, 310, 110, "Auth Service",
      "Identity · tokens · RBAC", "security")
    n(d, "ci-request", 1480, 430, 310, 125, "Request Service",
      "Validate · deduplicate · persist · status", "core")
    n(d, "ci-async", 1090, 650, 300, 135, "Async Pipeline",
      "Outbox · RabbitMQ · Workers", "messaging")
    n(d, "ci-domain", 1480, 650, 310, 135, "Optional Business Adapter",
      "Replaceable client-domain execution", "external", dashed=True)

    for idx, (target, target_y) in enumerate((("ci-auth", 312), ("ci-submit", 502), ("ci-status", 702)), 1):
        e(d, f"ci-client-{idx}", "ci-clients-list", target, "", color="#94A3B8",
          points=[(470, 530), (550, 530), (550, target_y), (620, target_y)])
    e(d, "ci-1", "ci-auth", "ci-gateway", "",
      points=[(940, 312), (1010, 312), (1010, 380), (1090, 380)])
    e(d, "ci-2", "ci-submit", "ci-gateway", "",
      points=[(940, 502), (1030, 502), (1030, 415), (1090, 415)])
    e(d, "ci-3", "ci-status", "ci-gateway", "",
      points=[(940, 702), (1050, 702), (1050, 455), (1090, 455)])
    e(d, "ci-4", "ci-gateway", "ci-auth-service", "",
      points=[(1390, 380), (1440, 380), (1440, 315), (1480, 315)])
    e(d, "ci-5", "ci-gateway", "ci-request", "",
      points=[(1390, 450), (1480, 492)])
    e(d, "ci-6", "ci-request", "ci-async", "",
      points=[(1635, 555), (1635, 610), (1240, 610), (1240, 650)])
    e(d, "ci-7", "ci-async", "ci-domain", "", dashed=True,
      points=[(1390, 718), (1480, 718)])
    d.set_legend(("Replaceable client", "external"), ("Stable API", "edge"),
                 ("HLRMS Core", "core"), ("Async processing", "messaging"))
    return finish(d)


def diagram_fault_tolerance() -> Diagram:
    d = Diagram(
        "15-fault-tolerance/fault-tolerance-recovery",
        "Fault Tolerance and Recovery",
        "Different failures produce different availability behavior, but accepted work remains traceable.",
        "Reliability",
    )
    scenarios = [
        ("ft-rabbit", 60, "RabbitMQ Unavailable", "Request API remains available\nOutbox accumulates in PostgreSQL\nPublisher reconnects and drains later", "messaging"),
        ("ft-redis", 515, "Redis Unavailable", "Request Service remains ready\nDB-backed idempotency fallback\nCache performance is degraded", "data"),
        ("ft-worker", 970, "Worker Failure", "Message remains unacknowledged/redelivered\nListener retries\nTerminal failure routes to DLQ", "service"),
        ("ft-pg", 1425, "PostgreSQL Unavailable", "Admission is temporarily unavailable\nReadiness fails\nServices recover after DB restart", "failure"),
    ]
    for node_id, x, title, subtitle, kind in scenarios:
        n(d, node_id, x, 230, 395, 300, title, subtitle, kind, title_size=24, body_size=18, align="left")
    n(d, "ft-detect", 85, 680, 330, 125, "1 · Detect", "Health probes · errors · metrics", "ops")
    n(d, "ft-isolate", 540, 680, 330, 125, "2 · Isolate", "Outbox · queue · circuit breaker · readiness", "core")
    n(d, "ft-recover", 995, 680, 330, 125, "3 · Recover", "Restart/reconnect · retry · redispatch", "messaging")
    n(d, "ft-reconcile", 1450, 680, 330, 125, "4 · Reconcile", "Outbox=0 · Queue=0 · DLQ delta · row counts", "success")
    e(d, "ft-1", "ft-detect", "ft-isolate", "")
    e(d, "ft-2", "ft-isolate", "ft-recover", "")
    e(d, "ft-3", "ft-recover", "ft-reconcile", "")
    n(d, "ft-workflow", 540, 570, 840, 60,
      "Every injected failure is evaluated through the same recovery workflow", "", "ops", shape="pill", title_size=19)
    d.text("ft-rule", 290, 875, 1340, 60,
           "Reliability claim: every request that was durably accepted can be reconciled after recovery; availability during the outage depends on the failed dependency.",
           18, INK, True, "center")
    d.set_legend(("Broker failure", "messaging"), ("Cache failure", "data"),
                 ("Worker failure", "service"), ("Durable DB failure", "failure"),
                 ("Recovery proof", "success"))
    return finish(d)


def diagram_performance_architecture() -> Diagram:
    d = Diagram(
        "16-performance/performance-test-architecture",
        "Performance Test Architecture",
        "k6 drives controlled traffic while internal telemetry and reconciliation explain the result.",
        "Performance",
    )
    d.group("pa-load", 45, 185, 430, 740, "LOAD GENERATION", "Host A", "external", False)
    d.group("pa-sut", 510, 185, 910, 740, "SYSTEM UNDER TEST", "Host B", "core", False)
    d.group("pa-evidence", 1455, 185, 410, 740, "EVIDENCE", "Live and post-run", "ops", False)
    n(d, "pa-script", 100, 270, 320, 120, "Versioned k6 Script", "Scenario · target · duration · thresholds", "external")
    n(d, "pa-run", 100, 490, 320, 150, "k6 Runner", "Unique TEST_RUN_ID\nramping-arrival-rate\nsummary export", "external", title_size=24, body_size=18)
    n(d, "pa-output", 100, 745, 320, 105, "Raw Result Files", "JSON/CSV + terminal summary", "ops")
    n(d, "pa-lb", 565, 250, 270, 105, "Gateway LB", "Public test endpoint", "edge")
    n(d, "pa-admit", 930, 250, 420, 105, "Admission Tier", "Gateway + 2 Request Services", "core")
    n(d, "pa-buffer", 565, 500, 270, 130, "Durable Buffer", "Outbox + RabbitMQ", "messaging")
    n(d, "pa-workers", 930, 500, 420, 130, "Execution Tier", "Workers × N + final-state persistence", "service")
    n(d, "pa-data", 745, 750, 420, 105, "Shared State", "PostgreSQL + Redis", "data")
    n(d, "pa-graf", 1505, 270, 310, 130, "Grafana Live View", "Prometheus-backed · RPS · P95 · errors · queue · CPU", "ops")
    n(d, "pa-recon", 1505, 510, 310, 145, "Integrity Reconciliation", "accepted = outbox = published = processed", "data")
    n(d, "pa-report", 1505, 755, 310, 105, "Benchmark Record", "hardware · topology · result · limitation", "success")
    e(d, "pa-1", "pa-script", "pa-run", "configure")
    e(d, "pa-2", "pa-run", "pa-lb", "HTTP over LAN",
      points=[(420, 565), (490, 565), (490, 302), (565, 302)])
    e(d, "pa-3", "pa-lb", "pa-admit", "")
    e(d, "pa-4", "pa-admit", "pa-buffer", "",
      points=[(1140, 355), (1140, 430), (700, 430), (700, 500)])
    e(d, "pa-5", "pa-buffer", "pa-workers", "")
    e(d, "pa-6", "pa-workers", "pa-data", "",
      points=[(1140, 630), (1140, 690), (955, 690), (955, 750)])
    e(d, "pa-7", "pa-run", "pa-output", "export")
    e(d, "pa-8", "pa-admit", "pa-graf", "", dashed=True,
      points=[(1350, 302), (1425, 302), (1425, 335), (1505, 335)])
    e(d, "pa-10", "pa-data", "pa-recon", "",
      points=[(1165, 802), (1380, 802), (1380, 582), (1505, 582)])
    e(d, "pa-11", "pa-output", "pa-report", "client-side result",
      points=[(420, 798), (470, 798), (470, 895), (1450, 895), (1450, 807), (1505, 807)])
    e(d, "pa-12", "pa-recon", "pa-report", "server-side proof")
    d.set_legend(("Load generation", "external"), ("Edge/admission", "core"),
                 ("Buffering", "messaging"), ("Execution", "service"),
                 ("Evidence", "ops"))
    return finish(d)


def diagram_performance_evidence() -> Diagram:
    d = Diagram(
        "16-performance/performance-evidence-chain",
        "Performance Evidence Chain",
        "A random run identifier chosen at test time links every visible and persisted artifact.",
        "Performance",
    )
    n(d, "pe-run-id", 350, 185, 1220, 75, "One TEST_RUN_ID propagates through every artifact",
      "Chosen at test time; printed by k6 and embedded in requests, queries and screenshots.", "core",
      shape="pill", title_size=21, body_size=15)
    stages = [
        ("pe-id", 50, "1 · Generate Run ID", "DEFENSE-<UTC>-<random>\nShown before the run", "external"),
        ("pe-k6", 355, "2 · k6 Executes", "Target / effective RPS\niterations · drops · latency · failures", "external"),
        ("pe-live", 660, "3 · Grafana Live", "Gateway RPS\nqueue depth\nworker completion\nresource saturation", "ops"),
        ("pe-db", 965, "4 · Database Reconcile", "requests\noutbox_events\nprocessed_events\nunique IDs", "data"),
        ("pe-broker", 1270, "5 · Pipeline Drain", "Outbox pending = 0\nready = 0\nunacked = 0\nDLQ unchanged", "messaging"),
        ("pe-record", 1575, "6 · Final Record", "Script commit\ncommand\nhardware\nsummary + screenshots", "success"),
    ]
    for node_id, x, title, subtitle, kind in stages:
        n(d, node_id, x, 330, 260, 300, title, subtitle, kind, title_size=22, body_size=18, align="left")
    for idx in range(1, 6):
        e(d, f"pe-e-{idx}", f"pe-{['id','k6','live','db','broker'][idx-1]}",
          f"pe-{['k6','live','db','broker','record'][idx-1]}", "")
    n(d, "pe-proof", 400, 765, 1120, 140, "Verification Rule",
      "Accepted business requests = persisted requests = Outbox events = published events = processed events\nwith zero permanent backlog and no unexpected DLQ growth.",
      "core", title_size=25, body_size=19)
    e(d, "pe-final", "pe-record", "pe-proof", "", dashed=True,
      points=[(1705, 630), (1705, 700), (960, 700), (960, 765)])
    d.set_legend(("Test identity / client evidence", "external"), ("Live telemetry", "ops"),
                 ("Durable evidence", "data"), ("Broker evidence", "messaging"),
                 ("Verified record", "success"))
    return finish(d, "Use a committee-chosen random suffix to make pre-recording impossible")


def diagram_outbox_idempotency() -> Diagram:
    d = Diagram(
        "17-reliability/outbox-idempotency",
        "Outbox and End-to-End Idempotency",
        "Producer-side atomicity and consumer-side deduplication close different failure gaps.",
        "Reliability",
    )
    d.group("oi-producer", 50, 190, 850, 570, "PRODUCER SIDE", "Request Service + Outbox Publisher", "core", False)
    d.group("oi-consumer", 1020, 190, 850, 570, "CONSUMER SIDE", "RabbitMQ + Request Worker", "service", False)
    n(d, "oi-client", 105, 285, 300, 115, "1 · Client Command", "User-scoped Idempotency-Key + payload", "external")
    n(d, "oi-lock", 515, 285, 300, 115, "2 · Distributed Coordination", "Redis lock / replay cache with DB fallback", "data", title_size=20)
    n(d, "oi-tx", 105, 520, 300, 150, "3 · Single DB Transaction", "INSERT request\nINSERT outbox event", "data")
    n(d, "oi-pub", 515, 520, 300, 150, "4 · Outbox Publisher", "Claim · publish · confirm · mark PUBLISHED", "messaging")
    n(d, "oi-rabbit", 1075, 285, 300, 115, "5 · At-Least-Once Delivery", "RabbitMQ may redeliver after failures", "messaging", "queue", title_size=20)
    n(d, "oi-register", 1485, 285, 300, 115, "6 · Register event_id", "INSERT ... ON CONFLICT DO NOTHING", "data")
    n(d, "oi-work", 1075, 520, 300, 150, "7 · Process Once", "Only a newly registered event runs business processing", "service")
    n(d, "oi-final", 1485, 520, 300, 150, "8 · Commit Outcome", "Final request state + processed_at + ACK", "success")
    e(d, "oi-1", "oi-client", "oi-lock", "")
    e(d, "oi-2", "oi-lock", "oi-tx", "")
    e(d, "oi-3", "oi-tx", "oi-pub", "")
    e(d, "oi-4", "oi-pub", "oi-rabbit", "")
    e(d, "oi-5", "oi-rabbit", "oi-register", "")
    e(d, "oi-6", "oi-register", "oi-work", "")
    e(d, "oi-7", "oi-work", "oi-final", "")
    d.text("oi-producer-rule", 160, 440, 590, 45,
           "Replay: same payload → original Request ID; different payload → 409", 16, MUTED, True, "center")
    d.text("oi-consumer-rule", 1130, 440, 590, 45,
           "Duplicate event_id → skip the business effect and ACK", 16, MUTED, True, "center")
    n(d, "oi-result", 250, 800, 1420, 95, "End-to-End Guarantee",
      "A request is never lost between PostgreSQL and RabbitMQ, and a redelivered event does not repeat its business effect.",
      "success", "pill", title_size=20, body_size=16)
    d.set_legend(("Client idempotency", "external"), ("Transactional guarantee", "data"),
                 ("Messaging guarantee", "messaging"), ("Worker guarantee", "service"),
                 ("Final result", "success"))
    return finish(d)


def diagram_repository_workflow() -> Diagram:
    d = Diagram(
        "18-repository/repository-and-delivery-workflow",
        "Repository and Documentation Delivery Workflow",
        "Supplementary project-management view; it is not part of the HLRMS runtime architecture.",
        "Delivery",
    )
    n(d, "rw-issues", 65, 330, 245, 145, "Planned Change", "Requirement · issue · architecture decision", "neutral")
    n(d, "rw-feature", 390, 330, 260, 145, "feature/* · fix/*", "Code, tests and local verification", "service")
    n(d, "rw-review", 730, 330, 250, 145, "Pull Request", "Peer review · tests · documentation update", "edge")
    n(d, "rw-develop", 1070, 330, 260, 145, "develop", "Integrated complete implementation", "core")
    n(d, "rw-docs", 1450, 330, 350, 145, "docs/final-delivery", "Documentation branch · final artifacts", "ops")
    n(d, "rw-main", 1450, 600, 350, 145, "main", "Approved stable graduation release", "success")
    e(d, "rw-1", "rw-issues", "rw-feature", "implement",
      points=[(310, 402), (390, 402)])
    e(d, "rw-2", "rw-feature", "rw-review", "open PR",
      points=[(650, 402), (730, 402)])
    e(d, "rw-3", "rw-review", "rw-develop", "merge",
      points=[(980, 402), (1070, 402)])
    e(d, "rw-4", "rw-develop", "rw-docs", "prepare delivery",
      points=[(1330, 380), (1450, 380)])
    e(d, "rw-5", "rw-docs", "rw-develop", "review", dashed=True,
      points=[(1450, 440), (1330, 440)])
    e(d, "rw-6", "rw-develop", "rw-main", "promote",
      points=[(1200, 475), (1200, 555), (1400, 555), (1400, 672), (1450, 672)])
    e(d, "rw-7", "rw-docs", "rw-main", "release contents",
      points=[(1625, 475), (1625, 600)])
    n(d, "rw-gate", 565, 720, 650, 140, "Merge Gate",
      "Automated tests pass · architecture matches implementation · generated artifacts reviewed · no secrets committed",
      "security", title_size=24, body_size=18)
    e(d, "rw-g1", "rw-review", "rw-gate", "quality gate", dashed=True,
      points=[(855, 475), (855, 720)])
    e(d, "rw-g2", "rw-gate", "rw-develop", "approved", dashed=True,
      points=[(1000, 720), (1000, 650), (1120, 650), (1120, 475)], label_dx=-65)
    d.set_legend(("Work branch", "service"), ("Review", "edge"),
                 ("Integration", "core"), ("Documentation", "ops"),
                 ("Stable release", "success"))
    return finish(d)


def diagram_api_routing() -> Diagram:
    d = Diagram(
        "19-api-routing/api-routing-and-ports",
        "API Routing and Ports",
        "Only the Gateway load balancer is required for client traffic; internal ports remain inside Docker.",
        "Integration",
    )
    n(d, "ar-client", 60, 390, 280, 140, "Client / k6", "HTTPS or LAN HTTP", "external")
    n(d, "ar-ngrok", 420, 220, 290, 110, "ngrok (Functional Demo)", "Public HTTPS → localhost:8088", "external", dashed=True)
    n(d, "ar-gwlb", 420, 500, 290, 110, "API Gateway LB", "Published :8088", "edge")
    n(d, "ar-gw", 810, 500, 290, 125, "API Gateway", "Internal :8088", "edge")
    n(d, "ar-auth", 1210, 210, 300, 125, "Auth Service", "Internal :8081\n/api/v1/auth/**", "security")
    n(d, "ar-rslb", 1210, 470, 300, 125, "Request Service LB", "Published for diagnostics :18080\ninternal :8080", "edge")
    n(d, "ar-demo", 1210, 735, 300, 125, "Demo Business API", "Internal :8090\nreplaceable", "external", dashed=True)
    n(d, "ar-rs", 1600, 470, 260, 125, "Request Service × 2", "Internal :8080", "core")
    n(d, "ar-graf", 1600, 210, 260, 105, "Grafana :3000", "Operator UI", "ops")
    n(d, "ar-prom", 1600, 735, 260, 105, "Prometheus :9090", "Metrics query UI", "ops")
    e(d, "ar-1", "ar-client", "ar-ngrok", "optional public demo", dashed=True)
    e(d, "ar-2", "ar-ngrok", "ar-gwlb", "")
    e(d, "ar-3", "ar-client", "ar-gwlb", "LAN / localhost")
    e(d, "ar-4", "ar-gwlb", "ar-gw", "")
    e(d, "ar-5", "ar-gw", "ar-auth", "/api/v1/auth/**")
    e(d, "ar-6", "ar-gw", "ar-rslb", "/api/v1/requests/**")
    e(d, "ar-7", "ar-rslb", "ar-rs", "round-robin")
    e(d, "ar-8", "ar-gw", "ar-demo", "optional demo routes", dashed=True)
    e(d, "ar-9", "ar-prom", "ar-graf", "datasource", dashed=True)
    d.text("ar-warning", 510, 870, 1330, 62,
           "Performance testing uses Host B's LAN address on :8088. ngrok is not part of the benchmark path.",
           19, "#9A3412", True, "center")
    d.set_legend(("External / optional", "external"), ("Published edge", "edge"),
                 ("Internal core", "core"), ("Identity", "security"),
                 ("Operations", "ops"))
    return finish(d)


def build_diagrams() -> list[Diagram]:
    return [
        diagram_system_context(),
        diagram_use_case_overview(),
        diagram_submit_use_case(),
        diagram_dfd_level0(),
        diagram_dfd_level1(),
        diagram_activity(),
        diagram_submission_sequence(),
        diagram_async_sequence(),
        diagram_state(),
        diagram_class(),
        diagram_c4_level1(),
        diagram_c4_level2(),
        diagram_c4_level3(),
        diagram_deployment_overview(),
        diagram_compose_topology(),
        diagram_two_host_topology(),
        diagram_auth_erd(),
        diagram_database_landscape(),
        diagram_requests_erd(),
        diagram_rabbit_topology(),
        diagram_message_lifecycle(),
        diagram_monitoring_architecture(),
        diagram_metrics_alerts(),
        diagram_security_boundaries(),
        diagram_client_integration(),
        diagram_fault_tolerance(),
        diagram_performance_architecture(),
        diagram_performance_evidence(),
        diagram_outbox_idempotency(),
        diagram_repository_workflow(),
        diagram_api_routing(),
    ]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--only", action="append", default=[])
    parser.add_argument("--no-export", action="store_true")
    args = parser.parse_args()

    selected = set(args.only)
    diagrams = [d for d in build_diagrams() if not selected or d.key in selected]
    temp_dir = Path(os.environ.get("HLRMS_DIAGRAM_TEMP_DIR", "/tmp/hlrms-diagram-svg"))
    temp_dir.mkdir(parents=True, exist_ok=True)

    for diagram in diagrams:
        rel = Path(diagram.key)
        folder = args.root / "diagrams" / rel.parent
        stem = rel.name
        drawio = folder / f"{stem}.drawio"
        svg = temp_dir / f"{rel.parent.as_posix().replace('/', '__')}__{stem}.svg"
        png = folder / f"{stem}.png"
        pdf = folder / f"{stem}.pdf"
        render_drawio(diagram, drawio)
        render_svg(diagram, svg)
        if not args.no_export:
            export(svg, png, pdf)
        print(f"generated {diagram.key}")


if __name__ == "__main__":
    main()
