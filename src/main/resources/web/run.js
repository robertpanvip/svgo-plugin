function optimizeSvg(svgString, options = []) {

    const extra = [
        "sortAttrs",
        "sortDefsChildren",
        "prefixIds",
        "reusePaths",
        "convertStyleToAttrs",
        "convertTransform",
        "removeDimensions",
        "mergeStyles",
        "cleanupListOfValues",
        "removeOffCanvasPaths",
        "removeRasterImages",
        "removeScripts",
        "removeStyleElement",
        "removeTitle",
        "removeViewBox",
        "removeMetadata",
        "convertEllipseToCircle",
        "removeEmptyContainers"
    ];

    // 选中的插件
    const enabledKeys = options
        .filter(item => item.checked)
        .map(item => item.key);

    // preset-default overrides
    const overrides = Object.fromEntries(
        options
            .filter(item => !extra.includes(item.key))
            .map(item => [
                item.key,
                enabledKeys.includes(item.key)
            ])
    );

    const result = window.svgo?.optimize?.(svgString, {
        multipass: true,
        plugins: [
            {
                name: "preset-default",
                params: {
                    overrides
                }
            },

            // 独立插件
            ...enabledKeys.filter(key => extra.includes(key))
        ]
    });

    return result?.data ?? svgString;
}