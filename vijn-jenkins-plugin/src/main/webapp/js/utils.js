/**
 * Calculate width of text rendered using assigned font
 * @param text Text that width is to be returned
 * @param font Font used for text render
 * @returns {*} Text width in pixels
 */
function textWidth(text, font) {
    let canvas = document.getElementById('computedTextWidth');
    if (!canvas) {
        canvas = document.createElement('canvas');
        canvas.id = 'computedTextWidth';
        canvas.style.cssText = 'visibility: hidden; position: absolute; left: -999em; top:-999em;';
        document.body.appendChild(canvas);
    }
    const context = canvas.getContext('2d');
    context.font = font;
    context.fillText(text, 0, 0);
    return context.measureText(text).width;
}