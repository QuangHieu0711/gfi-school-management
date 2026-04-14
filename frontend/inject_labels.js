const fs = require('fs');

const modelPath = 'src/app/shared/models/admin/hoc-sinh.model.ts';
let code = fs.readFileSync(modelPath, 'utf8');

// We want to add `label: '...'` below placeholder if it doesn't have a label.
// It will try to infer a good label from the item name or placeholder.

function getLabel(itemName, placeholder) {
    if (placeholder && !placeholder.includes('Chọn ')) {
        return placeholder;
    }
    const cleanPh = placeholder ? placeholder.replace('Chọn ', '').replace('Tỉnh/TP', 'Tỉnh/TP').replace('Xã/Phường', 'Xã/Phường') : '';
    if (cleanPh) return cleanPh;
    return itemName;
}

code = code.replace(/(\w+Item)\s*:\s*(SELECT_CONTROL|DATE_CONTROL|TEXT_CONTROL)\(\{\s*controlName:\s*'[^']+',\s*(?:label:\s*'[^']+',\s*)?placeholder:\s*'([^']+)',/g, (match, itemName, ctlType, placeholder) => {
    // Note: if label already exists, we might match it above but maybe we don't.
    // Let's use a simpler replace
    return match;
});

// A better way: replace block by block
let modified = [];
let re = /((?:\w+Item)\s*:\s*(?:SELECT_CONTROL|DATE_CONTROL|TEXT_CONTROL)\s*\(\{)([\s\S]*?)(\}\),)/g;
code = code.replace(re, (match, start, inner, end) => {
    if (!inner.includes('label:')) {
        let phMatch = inner.match(/placeholder:\s*'([^']+)'/);
        if (phMatch) {
            let ph = phMatch[1];
            let label = ph.replace(/Chọn\s+/i, '');
            // Capitalize first letter of label just in case
            label = label.charAt(0).toUpperCase() + label.slice(1);
            return `${start}\n    label: '${label}',${inner}${end}`;
        }
    }
    return match;
});

fs.writeFileSync(modelPath, code, 'utf8');
console.log("Labels added.");
