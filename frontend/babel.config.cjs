/** @type {import('@babel/core').TransformOptions} */
module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' }, modules: 'commonjs' }],
    ['@babel/preset-react', { runtime: 'automatic' }],
  ],
  plugins: [
    /**
     * Transforms `import.meta.env` for Jest.
     *
     * Vite uses `import.meta.env` for env vars, but Jest uses CommonJS and
     * does not understand `import.meta`. This plugin replaces every occurrence
     * of `import.meta` with a plain object that mirrors the Vite env shape,
     * allowing tests to run without a Vite build.
     */
    function transformImportMeta({ types: t }) {
      return {
        visitor: {
          MetaProperty(path) {
            if (
              path.node.meta.name === 'import' &&
              path.node.property.name === 'meta'
            ) {
              path.replaceWith(
                t.objectExpression([
                  t.objectProperty(
                    t.identifier('env'),
                    t.objectExpression([
                      t.objectProperty(
                        t.identifier('VITE_API_BASE'),
                        t.logicalExpression(
                          '||',
                          t.memberExpression(
                            t.memberExpression(
                              t.identifier('process'),
                              t.identifier('env')
                            ),
                            t.identifier('VITE_API_BASE')
                          ),
                          t.stringLiteral('http://localhost:8000')
                        )
                      ),
                    ])
                  ),
                ])
              );
            }
          },
        },
      };
    },
  ],
};
