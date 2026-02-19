/**
 * https://github.com/xzitlou/jsontr.ee - MIT License
 * Generates an SVG visualization of a JSON object as a tree.
 * @param {Object} json - The JSON object to be visualized.
 * @param {Object} options - The customization options for the chart.
 * @returns {string} - A string containing the generated SVG.
 */
function generateJSONTree(json, options = {}) {
    const defaultOptions = {
        arrowColor: "#475872",
        nodeBorderColor: "#475872",
        nodeBackgroundColor: "#f6f8fa",
        keyColor: "#b16b2a",
        valueColor: "#008000",
        padding: 10,
        lineHeight: 18,
        fontSize: 12,
        fontFamily: "monospace",
    };
    const config = {...defaultOptions, ...options};
    const {arrowColor, nodeBorderColor, nodeBackgroundColor, keyColor, valueColor, padding, lineHeight, fontSize, fontFamily} = config;
    let svgContent = []; // SVG content to be generated
    let edges = []; // Stores connections between nodes
    let nodeId = 0; // Counter to identify nodes
    let maxX = 0; // Maximum width reached
    let maxY = 0; // Maximum height reached
    const occupiedPositions = []; // Record of occupied positions to avoid overlaps

    /**
     * Measures the width of the given text.
     * @param {string} text - The text to measure.
     * @returns {number} - The width of the text in pixels.
     */
    function measureTextWidth(text) {
        const canvas = document.createElement("canvas");
        const context = canvas.getContext("2d");
        context.font = `${fontSize}px ${fontFamily}`;
        return context.measureText(text).width; // Returns the measured width
    }

    /**
     * Calculates the size of a node based on its content.
     * @param {Object} obj - The JSON object to calculate the size for.
     * @returns {Object} - An object with the width, height, and lines of the node.
     */
    function calculateNodeSize(obj) {
        const lines = []; // Stores the lines to be displayed in the node

        // Handling arrays
        if (Array.isArray(obj)) {
            lines.push({key: `(${obj.length})`, value: "Array"});
        } else if (typeof obj === "object" && obj !== null) {
            // Handling objects
            if (Object.keys(obj).length === 0) {
                lines.push({key: "", value: "{}"}); // Handling empty object
            } else {
                // Iterating over object properties
                for (const [key, value] of Object.entries(obj)) {
                    const displayValue = Array.isArray(value)
                        ? `Array (${value.length})` // Display array length
                        : typeof value === "object"
                            ? "{}" // Indicate it's an object
                            : JSON.stringify(value); // Convert to JSON string
                    lines.push({key, value: displayValue}); // Add line
                }
            }
        } else {
            lines.push({key: "", value: JSON.stringify(obj)}); // For primitive values
        }

        const maxWidth = Math.max(...lines.map(line => measureTextWidth(`${line.key}: ${line.value}`)), 0);
        const height = lines.length * lineHeight + padding * 2; // Total node height

        return {width: Math.max(maxWidth + padding * 2, 0), height: Math.max(height, 0), lines}; // Returns node size
    }

    /**
     * Adjusts the position of a node and avoids overlaps with other nodes.
     * @param {number} x - Initial X position of the node.
     * @param {number} y - Initial Y position of the node.
     * @param {number} width - Width of the node.
     * @param {number} height - Height of the node.
     * @returns {number} - The adjusted Y position to avoid overlaps.
     */
    function adjustPosition(x, y, width, height) {
        let adjustedY = y; // Initially, Y equals the provided Y
        const buffer = 10; // Additional spacing to avoid overlap

        // Check if the new position collides with occupied positions
        for (const pos of occupiedPositions) {
            const [ox, oy, ow, oh] = pos;
            if (
                x < ox + ow &&
                x + width > ox &&
                adjustedY < oy + oh &&
                adjustedY + height > oy
            ) {
                // If there's a collision, adjust downward
                adjustedY = oy + oh + buffer;
            }
        }

        // Register the occupied position
        occupiedPositions.push([x, adjustedY, width, height]);
        return adjustedY; // Return the adjusted position
    }

    /**
     * Recursive function to build the node tree.
     * @param {Object} obj - The JSON object to be visualized.
     * @param {number} x - The X position of the node.
     * @param {number} y - The Y position of the node.
     * @param {string|null} parentId - ID of the parent node (if exists).
     * @param {Object|null} parentPosition - Position of the parent node (if exists).
     */
    function buildTree(obj, x, y, parentId = null, parentPosition = null) {
        const {width, height, lines} = calculateNodeSize(obj); // Calculate node size
        const adjustedX = adjustPosition(y, x, height, width); // Adjust X position (swap x/y for vertical)
        const currentId = `node-${nodeId++}`; // Create unique ID for the node

        // Generate node content using divs and display:flex
        const nodeContent = lines.map(line => `
            <div style="display: flex;">
                <span class="json-key" style="margin-right: 5px; color: ${keyColor}">${line.key ? `${line.key}:` : ""}</span>
                <span class="json-value" style="color: ${valueColor}">${line.value}</span>
            </div>
        `).join("");

        // Add the node to the SVG content (swapped coordinates for vertical layout)
        svgContent.push(`
        <g id="${currentId}" transform="translate(${adjustedX}, ${y})">
            <rect width="${width}" height="${height}" rx="5" ry="5" style="fill:${nodeBackgroundColor};stroke:${nodeBorderColor};stroke-width:1"></rect>
            <foreignObject width="${width}" height="${height}">
                <div xmlns="http://www.w3.org/1999/xhtml" style="font-family:${fontFamily}; font-size:${fontSize}px; line-height:${lineHeight}px; padding:${padding}px; box-sizing:border-box;">
                    ${nodeContent}
                </div>
            </foreignObject>
        </g>
    `);

        // If it has a parent node, add a dynamic arrow (vertical layout)
        if (parentId && parentPosition) {
            const parentCenterX = parentPosition.x + parentPosition.width / 2; // Horizontal center of parent node
            const parentBottomY = parentPosition.y + parentPosition.height; // Bottom of parent node
            const childCenterX = adjustedX + width / 2; // Horizontal center of child node
            const childTopY = y; // Top of child node

            const verticalPadding = 10; // Vertical space before curve

            // Add the connection between the parent node and the child (vertical)
            edges.push(`
            <path d="M${parentCenterX},${parentBottomY} C${parentCenterX},${(parentBottomY + childTopY) / 2} ${childCenterX},${(parentBottomY + childTopY) / 2} ${childCenterX},${childTopY}"
                  style="fill:none;stroke:${arrowColor};stroke-width:1;marker-end:url(#arrowhead);" />
        `);
        }

        let nextXOffset = adjustedX; // Initialize the next X position

        // Iterate over the lines to build child nodes
        lines.forEach(line => {
            const key = line.key; // Get node key

            if (key && obj.hasOwnProperty(key)) { // Check if the object has the key
                const value = obj[key]; // Get the corresponding value
                const childY = y + height + 100; // Y position for the child (vertical layout)

                // Handling arrays
                if (Array.isArray(value)) {
                    const listNode = {[`${line.key} (${value.length})`]: "Array"}; // Node for the array
                    const listX = nextXOffset; // X for the array node

                    buildTree(listNode, listX, childY, currentId, {x: adjustedX, y, width, height}); // Build node for the array

                    value.forEach((item, index) => {
                        const childX = nextXOffset + index * (width + 50); // X position for each item in the array
                        buildTree(item, childX, childY + calculateNodeSize(listNode).height + 100, `node-${nodeId - 1}`, {
                            x: listX,
                            y: childY,
                            width: calculateNodeSize(listNode).width,
                            height: calculateNodeSize(listNode).height,
                        });
                    });

                    nextXOffset += value.length * (width + 50) + 50; // Adjust X for the next node
                } else if (typeof value === "object" && value !== null) {
                    const nestedParentNode = {[line.key]: "Object"}; // Node for the nested object
                    const nestedParentX = nextXOffset; // X for the nested parent node

                    // Create a node for the property name that contains the nested object
                    buildTree(nestedParentNode, nestedParentX, childY, currentId, {x: adjustedX, y, width, height});

                    // Create the child nodes of the nested object
                    buildTree(value, nestedParentX, childY + calculateNodeSize(nestedParentNode).height + 100, `node-${nodeId - 1}`, {
                        x: nestedParentX,
                        y: childY,
                        width: calculateNodeSize(nestedParentNode).width,
                        height: calculateNodeSize(nestedParentNode).height,
                    });

                    nextXOffset += calculateNodeSize(value).width + 50; // Adjust X after processing the nested object
                }
            } else if (Array.isArray(obj)) { // Check if the root of the JSON is an Array, then render the child nodes
                const listX = nextXOffset; // X for the array node
                obj.forEach((item, index) => {
                    const childY = y + height + 100; // Y position for the child

                    if (typeof item === "object" && item !== null) {
                        // Create the child nodes of the nested object
                        buildTree(item, nextXOffset, childY, currentId, {
                            x: adjustedX,
                            y: y,
                            width: width,
                            height: height,
                        });

                        nextXOffset += calculateNodeSize(item).width + 50; // Adjust X after processing the nested object
                    } else if (Array.isArray(item)) {
                        const listNode = {[`(${item.length})`]: "Array"}; // Node for the array

                        buildTree(listNode, nextXOffset, childY, currentId, {x: adjustedX, y, width, height}); // Build node for the array

                        item.forEach((subitem, subindex) => {
                            const childX = nextXOffset + subindex * (width + 50); // X position for each item in the array
                            buildTree(subitem, childX, childY + calculateNodeSize(listNode).height + 100, `node-${nodeId - 1}`, {
                                x: nextXOffset,
                                y: childY,
                                width: calculateNodeSize(listNode).width,
                                height: calculateNodeSize(listNode).height,
                            });
                        });

                        nextXOffset += item.length * (width + 50) + 50; // Adjust X for the next node
                    } else {
                        buildTree(item, nextXOffset, childY, currentId, {
                            x: adjustedX,
                            y: y,
                            width: width,
                            height: height,
                        });

                        nextXOffset += calculateNodeSize(item).width + 50;
                    }
                });
            }
        });

        // Adjust `maxY` and `maxX` for the SVG
        maxY = Math.max(maxY, y + height);
        // maxX = Math.max(maxX, nextXOffset); <-- TODO: JSONs with root larger than child nodes generate clipped SVG
        maxX = Math.max(maxX, adjustedX + width);
    }

    // Start building the tree with the provided JSON
    buildTree(json, 50, 50);

    // Return the generated SVG content
    return `
        <svg xmlns="http://www.w3.org/2000/svg" width="${maxX + 150}" height="${maxY + 150}">
            <defs>
                <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" style="fill:${arrowColor};" />
                </marker>
            </defs>
            ${edges.join("")}
            ${svgContent.join("")}
        </svg>
    `;
}