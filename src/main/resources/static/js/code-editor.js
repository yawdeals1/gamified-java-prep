(function () {
    'use strict';

    const INDENT_SIZE = 4;

    function leadingColumns(line) {
        let columns = 0;
        for (const character of line) {
            if (character === ' ') columns += 1;
            else if (character === '\t') columns += INDENT_SIZE - (columns % INDENT_SIZE);
            else break;
        }
        return columns;
    }

    function activeLine(textarea) {
        return textarea.value.slice(0, textarea.selectionStart || 0).split('\n').length;
    }

    function enhance(textarea) {
        if (!textarea || textarea.dataset.codeEditorReady === 'true') return textarea;

        const shell = document.createElement('div');
        shell.className = 'java-code-editor';
        if (textarea.hasAttribute('data-code-editor-frame')) {
            shell.classList.add('java-code-editor--framed');
        }

        const gutter = document.createElement('div');
        gutter.className = 'java-code-editor__gutter';
        gutter.setAttribute('aria-hidden', 'true');
        const gutterContent = document.createElement('div');
        gutterContent.className = 'java-code-editor__gutter-content';
        gutter.appendChild(gutterContent);

        const viewport = document.createElement('div');
        viewport.className = 'java-code-editor__viewport';
        const guides = document.createElement('div');
        guides.className = 'java-code-editor__guides';
        guides.setAttribute('aria-hidden', 'true');
        const guideContent = document.createElement('div');
        guideContent.className = 'java-code-editor__guide-content';
        guides.appendChild(guideContent);

        textarea.parentNode.insertBefore(shell, textarea);
        shell.appendChild(gutter);
        shell.appendChild(viewport);
        viewport.appendChild(guides);
        viewport.appendChild(textarea);
        textarea.dataset.codeEditorReady = 'true';
        textarea.setAttribute('wrap', 'off');

        function applyMetrics() {
            const style = window.getComputedStyle(textarea);
            const lineHeight = parseFloat(style.lineHeight) || 21;
            gutter.style.fontFamily = style.fontFamily;
            gutter.style.fontSize = style.fontSize;
            gutter.style.lineHeight = lineHeight + 'px';
            gutterContent.style.paddingTop = style.paddingTop;
            gutterContent.style.paddingBottom = style.paddingBottom;
            guideContent.style.paddingTop = style.paddingTop;
            guideContent.style.paddingBottom = style.paddingBottom;
            guideContent.style.paddingLeft = style.paddingLeft;
            guideContent.style.paddingRight = style.paddingRight;
            guideContent.style.fontFamily = style.fontFamily;
            guideContent.style.fontSize = style.fontSize;
            guideContent.style.letterSpacing = style.letterSpacing;
            guideContent.style.lineHeight = lineHeight + 'px';
            return lineHeight;
        }

        function render() {
            const lineHeight = applyMetrics();
            const lines = textarea.value.split('\n');
            const selectedLine = activeLine(textarea);
            const numberFragment = document.createDocumentFragment();
            const guideFragment = document.createDocumentFragment();

            lines.forEach(function (line, index) {
                const number = document.createElement('span');
                number.className = 'java-code-editor__line-number' +
                    (index + 1 === selectedLine ? ' is-active' : '');
                number.style.height = lineHeight + 'px';
                number.textContent = String(index + 1);
                numberFragment.appendChild(number);

                const guideRow = document.createElement('div');
                guideRow.className = 'java-code-editor__guide-row';
                guideRow.style.height = lineHeight + 'px';
                const levels = Math.floor(leadingColumns(line) / INDENT_SIZE);
                for (let level = 1; level <= levels; level += 1) {
                    const guide = document.createElement('span');
                    guide.className = 'java-code-editor__indent-guide';
                    guide.style.left = 'calc(' + (level * INDENT_SIZE) + 'ch - 0.5px)';
                    guideRow.appendChild(guide);
                }
                guideFragment.appendChild(guideRow);
            });

            gutterContent.replaceChildren(numberFragment);
            guideContent.replaceChildren(guideFragment);
            guideContent.style.width = Math.max(textarea.clientWidth, textarea.scrollWidth) + 'px';
            syncScroll();
        }

        function updateActiveLine() {
            const selectedLine = activeLine(textarea);
            gutterContent.querySelectorAll('.java-code-editor__line-number').forEach(function (number, index) {
                number.classList.toggle('is-active', index + 1 === selectedLine);
            });
        }

        function syncScroll() {
            gutterContent.style.transform = 'translateY(' + (-textarea.scrollTop) + 'px)';
            guideContent.style.transform = 'translate(' + (-textarea.scrollLeft) + 'px, ' + (-textarea.scrollTop) + 'px)';
        }

        textarea.addEventListener('input', render);
        textarea.addEventListener('scroll', syncScroll, { passive: true });
        textarea.addEventListener('click', updateActiveLine);
        textarea.addEventListener('keyup', function (event) {
            // Existing editors insert indentation programmatically on Tab, which
            // does not emit an input event in the browser.
            if (event.key === 'Tab') render();
            else updateActiveLine();
        });
        textarea.addEventListener('select', updateActiveLine);
        if (typeof ResizeObserver !== 'undefined') {
            new ResizeObserver(function () { applyMetrics(); syncScroll(); }).observe(textarea);
        }

        textarea._javaCodeEditorRefresh = render;
        render();
        return textarea;
    }

    function refresh(textarea) {
        const editor = enhance(textarea);
        if (editor && editor._javaCodeEditorRefresh) editor._javaCodeEditorRefresh();
    }

    function enhanceAll(root) {
        (root || document).querySelectorAll('textarea[data-code-editor]').forEach(enhance);
    }

    window.JavaCodeEditor = { enhance: enhance, enhanceAll: enhanceAll, refresh: refresh };
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { enhanceAll(document); });
    } else {
        enhanceAll(document);
    }
}());
