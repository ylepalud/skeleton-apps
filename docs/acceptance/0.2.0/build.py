"""Renders the acceptance test book of content.py: a PDF with a clickable table of contents and
bookmarks and, for each test, fillable Pass, Fail and Blocked boxes and a notes field.

    python3 -m venv .venv && .venv/bin/pip install reportlab
    .venv/bin/python build.py <output.pdf> "<revision under test>"
"""

import sys
from datetime import date
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

from content import SECTIONS

OUTPUT = sys.argv[1]
REVISION = sys.argv[2] if len(sys.argv) > 2 else "develop"

INK = colors.HexColor("#1f2937")
MUTED = colors.HexColor("#6b7280")
ACCENT = colors.HexColor("#1d4ed8")
CODE_BG = colors.HexColor("#f3f4f6")
RULE = colors.HexColor("#d1d5db")

base = getSampleStyleSheet()
S = {
    "title": ParagraphStyle("title", parent=base["Title"], fontSize=26, leading=32, textColor=INK, spaceAfter=6 * mm),
    "subtitle": ParagraphStyle("subtitle", parent=base["Normal"], fontSize=13, leading=18, textColor=MUTED, alignment=TA_CENTER),
    "h1": ParagraphStyle("h1", parent=base["Heading1"], fontSize=18, leading=22, textColor=ACCENT, spaceBefore=0, spaceAfter=4 * mm),
    "h2": ParagraphStyle("h2", parent=base["Heading2"], fontSize=12.5, leading=16, textColor=INK, spaceBefore=5 * mm, spaceAfter=2 * mm),
    "body": ParagraphStyle("body", parent=base["Normal"], fontSize=9.5, leading=13.5, textColor=INK),
    "label": ParagraphStyle("label", parent=base["Normal"], fontName="Helvetica-Bold", fontSize=8.5, leading=12, textColor=MUTED, spaceBefore=2 * mm),
    "item": ParagraphStyle("item", parent=base["Normal"], fontSize=9.5, leading=13.5, textColor=INK, leftIndent=5 * mm, bulletIndent=1 * mm),
    "code": ParagraphStyle(
        "code", parent=base["Code"], fontName="Courier", fontSize=8.2, leading=10.5, textColor=INK,
        backColor=CODE_BG, borderPadding=(2, 3, 2, 3), leftIndent=7 * mm, rightIndent=1 * mm,
        spaceBefore=1.2 * mm, spaceAfter=1.2 * mm, wordWrap="CJK",
    ),
    "contents": ParagraphStyle("contents", parent=base["Heading1"], fontSize=18, leading=22, textColor=ACCENT, spaceAfter=4 * mm),
    "toc1": ParagraphStyle("toc1", fontName="Helvetica-Bold", fontSize=11, leading=17, leftIndent=0, textColor=INK),
    "toc2": ParagraphStyle("toc2", fontName="Helvetica", fontSize=9.5, leading=13, leftIndent=7 * mm, textColor=INK),
}


class Book(BaseDocTemplate):
    """Feeds the table of contents and the PDF outline from the headings, each linked to its page."""

    def __init__(self, filename):
        super().__init__(
            filename, pagesize=A4, leftMargin=18 * mm, rightMargin=18 * mm, topMargin=18 * mm, bottomMargin=18 * mm,
            title="skeleton-apps acceptance test book", author="skeleton-apps", subject="Acceptance tests",
        )
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="main")
        self.addPageTemplates([PageTemplate(id="page", frames=[frame], onPage=self.decorate)])

    def decorate(self, canvas, doc):
        if doc.page == 1:
            return
        canvas.saveState()
        canvas.setFont("Helvetica", 8)
        canvas.setFillColor(MUTED)
        canvas.drawString(18 * mm, 10 * mm, "skeleton-apps - acceptance test book")
        canvas.drawRightString(A4[0] - 18 * mm, 10 * mm, f"Page {doc.page}")
        canvas.restoreState()

    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph) or flowable.style.name not in ("h1", "h2"):
            return
        level = 0 if flowable.style.name == "h1" else 1
        text = flowable.getPlainText()
        # Stable across the passes of multiBuild: every heading's text is unique.
        key = "h-" + "".join(ch if ch.isalnum() else "-" for ch in text)
        self.canv.bookmarkPage(key)
        self.canv.addOutlineEntry(text, key, level=level, closed=level > 0)
        self.notify("TOCEntry", (level, text, self.page, key))


class ResultBox(Flowable):
    """The tester's verdict for one test: Pass, Fail and Blocked boxes and a notes field, fillable in any PDF reader."""

    HEIGHT = 22 * mm

    def __init__(self, test_id):
        super().__init__()
        self.test_id = test_id

    def wrap(self, available_width, available_height):
        self.width = available_width
        return available_width, self.HEIGHT

    def draw(self):
        c = self.canv
        c.setStrokeColor(RULE)
        c.setFillColor(colors.HexColor("#f9fafb"))
        c.roundRect(0, 1 * mm, self.width, self.HEIGHT - 2 * mm, 2 * mm, stroke=1, fill=1)
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 9)
        c.drawString(4 * mm, self.HEIGHT - 7.5 * mm, "Result")
        x = 22 * mm
        for value, label, color in (("pass", "Pass", "#15803d"), ("fail", "Fail", "#b91c1c"), ("blocked", "Blocked", "#b45309")):
            c.acroForm.checkbox(
                name=f"{self.test_id}-{value}", checked=False, x=x, y=self.HEIGHT - 9.5 * mm, size=11,
                buttonStyle="check", borderColor=colors.HexColor(color), fillColor=colors.white,
                textColor=colors.HexColor(color), borderWidth=1, forceBorder=True, relative=True,
                tooltip=f"{self.test_id}: {label}",
            )
            c.setFont("Helvetica", 9)
            c.setFillColor(colors.HexColor(color))
            c.drawString(x + 5.5 * mm, self.HEIGHT - 7.5 * mm, label)
            x += 26 * mm
        c.setFillColor(MUTED)
        c.setFont("Helvetica", 8)
        c.drawString(4 * mm, 6.5 * mm, "Notes")
        c.acroForm.textfield(
            name=f"{self.test_id}-notes", x=16 * mm, y=3 * mm, width=self.width - 20 * mm, height=8.5 * mm,
            fontSize=8, fieldFlags="multiline", borderColor=RULE, fillColor=colors.white, textColor=INK,
            borderWidth=0.5, relative=True, tooltip=f"{self.test_id}: notes",
        )


class FormField(Flowable):
    """A labelled one-line text field, for the cover and the sign-off."""

    def __init__(self, name, label, width=110 * mm):
        super().__init__()
        self.name, self.label, self.field_width = name, label, width

    def wrap(self, available_width, available_height):
        return available_width, 11 * mm

    def draw(self):
        c = self.canv
        c.setFont("Helvetica-Bold", 9.5)
        c.setFillColor(INK)
        c.drawString(0, 4 * mm, self.label)
        c.acroForm.textfield(
            name=self.name, x=45 * mm, y=1.5 * mm, width=self.field_width, height=7.5 * mm, fontSize=10,
            borderColor=RULE, fillColor=colors.white, textColor=INK, borderWidth=0.6, relative=True,
        )


def text(value):
    return escape(value)


def test_block(test):
    parts = [
        Paragraph(f"{text(test['id'])} - {text(test['title'])}", S["h2"]),
        Paragraph("Preconditions", S["label"]),
        Paragraph(text(test["pre"]), S["body"]),
        Paragraph("Steps", S["label"]),
    ]
    number = 0
    for step in test["steps"]:
        if step.startswith("$ "):
            parts.append(Paragraph(text(step), S["code"]))
        else:
            number += 1
            parts.append(Paragraph(text(step), S["item"], bulletText=f"{number}."))
    parts.append(Paragraph("Expected result", S["label"]))
    for expectation in test["expect"]:
        parts.append(Paragraph(text(expectation), S["item"], bulletText="-"))
    parts.append(Spacer(1, 2 * mm))
    parts.append(ResultBox(test["id"]))
    return KeepTogether(parts)


def summary_table():
    rows = [["Section", "Tests", "Passed", "Failed", "Blocked"]]
    for section in SECTIONS:
        rows.append([f"{section['id']} - {section['title']}", str(len(section["tests"])), "", "", ""])
    total = sum(len(section["tests"]) for section in SECTIONS)
    rows.append(["Total", str(total), "", "", ""])
    table = Table(rows, colWidths=[80 * mm, 18 * mm, 24 * mm, 24 * mm, 24 * mm], rowHeights=9 * mm)
    table.setStyle(TableStyle([
        ("FONT", (0, 0), (-1, 0), "Helvetica-Bold", 9.5),
        ("FONT", (0, 1), (-1, -1), "Helvetica", 9.5),
        ("FONT", (0, -1), (-1, -1), "Helvetica-Bold", 9.5),
        ("TEXTCOLOR", (0, 0), (-1, -1), INK),
        ("BACKGROUND", (0, 0), (-1, 0), CODE_BG),
        ("GRID", (0, 0), (-1, -1), 0.5, RULE),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (1, 0), (-1, -1), "CENTER"),
    ]))
    return table


class CountFields(Flowable):
    """Fillable counts over the summary table's Passed, Failed and Blocked columns."""

    def __init__(self, rows):
        super().__init__()
        self.rows = rows

    def wrap(self, available_width, available_height):
        return available_width, 0

    def draw(self):
        top = (self.rows + 1) * 9 * mm
        for index in range(self.rows):
            y = top - (index + 2) * 9 * mm + 1.5 * mm
            for column, x in enumerate((98 * mm, 122 * mm, 146 * mm)):
                self.canv.acroForm.textfield(
                    name=f"summary-{index}-{column}", x=x + 2 * mm, y=y, width=20 * mm, height=6 * mm, fontSize=9,
                    borderWidth=0, fillColor=colors.white, textColor=INK, relative=True,
                )


def story():
    total = sum(len(section["tests"]) for section in SECTIONS)
    flow = [
        Spacer(1, 40 * mm),
        Paragraph("Acceptance test book", S["title"]),
        Paragraph(
            "skeleton-apps on the local stack: " + ", ".join(section["title"] for section in SECTIONS if section["id"] != "T"),
            S["subtitle"],
        ),
        Spacer(1, 6 * mm),
        Paragraph(f"Revision under test: {text(REVISION)} - generated on {date.today().isoformat()} - {total} tests", S["subtitle"]),
        Spacer(1, 30 * mm),
        FormField("cover-tester", "Tester"),
        FormField("cover-date", "Date of the run"),
        FormField("cover-machine", "Machine and macOS"),
        Spacer(1, 20 * mm),
        Paragraph(
            "How to use this book: run the tests in order, section by section. Each test lists its preconditions, "
            "the exact steps (commands in grey boxes, to type from the root of the repository unless said "
            "otherwise) and the expected result. Tick one box per test: Pass when everything expected happens, Fail when "
            "something differs (say what in Notes), Blocked when a precondition could not be met. The buttons "
            "and fields can be filled in Preview, Acrobat or any PDF reader with forms: save the file to keep "
            "them. The table of contents and the bookmarks lead to each test.",
            S["body"],
        ),
        PageBreak(),
        Paragraph("Contents", S["contents"]),
    ]
    toc = TableOfContents(dotsMinLevel=0)
    toc.levelStyles = [S["toc1"], S["toc2"]]
    flow += [toc, PageBreak()]
    for section in SECTIONS:
        flow.append(Paragraph(f"{section['id']}. {text(section['title'])}", S["h1"]))
        flow.append(Paragraph(text(section["intro"]), S["body"]))
        for test in section["tests"]:
            flow.append(test_block(test))
        flow.append(PageBreak())
    flow += [
        Paragraph("Summary and sign-off", S["h1"]),
        Paragraph("Count the results of each section, then record the verdict.", S["body"]),
        Spacer(1, 4 * mm),
        CountFields(len(SECTIONS) + 1),
        summary_table(),
        Spacer(1, 10 * mm),
        FormField("signoff-verdict", "Verdict"),
        Paragraph("Accepted, accepted with reservations, or rejected; list the failed tests that motivate it.", S["label"]),
        Spacer(1, 4 * mm),
        FormField("signoff-reservations", "Reservations", width=125 * mm),
        FormField("signoff-name", "Name"),
        FormField("signoff-date", "Date"),
    ]
    return flow


Book(OUTPUT).multiBuild(story())
print(OUTPUT)
