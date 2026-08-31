function defaultItems(pageData) {
  if (Array.isArray(pageData)) return pageData
  return Array.isArray(pageData && pageData.items) ? pageData.items : []
}

async function loadAllPages(fetchPage, options = {}) {
  const pageSize = Math.max(1, Number(options.pageSize) || 100)
  const keyOf = options.keyOf || ((item) => item && item.id)
  const itemsOf = options.itemsOf || defaultItems
  const maxPages = Math.max(1, Number(options.maxPages) || 1000)
  const maxStagnantPages = Math.max(1, Number(options.maxStagnantPages) || 3)
  const rows = []
  const seen = new Set()
  let stagnantPages = 0

  for (let page = 1; page <= maxPages; page += 1) {
    const pageData = await fetchPage({ page, pageSize })
    const pageRows = itemsOf(pageData)
    let added = 0
    pageRows.forEach((item, index) => {
      const rawKey = keyOf(item, index)
      const key = rawKey === undefined || rawKey === null ? `page:${page}:row:${index}` : String(rawKey)
      if (seen.has(key)) return
      seen.add(key)
      rows.push(item)
      added += 1
    })

    const total = Number(pageData && !Array.isArray(pageData) ? pageData.total : NaN)
    if (Number.isFinite(total)) {
      if (rows.length >= total) return rows
      stagnantPages = added === 0 ? stagnantPages + 1 : 0
      if (pageRows.length === 0 || stagnantPages >= maxStagnantPages) {
        throw new Error(`分页数据不完整：期望 ${total} 条，实际加载 ${rows.length} 条`)
      }
      continue
    }
    if (pageRows.length < pageSize || pageRows.length === 0 || added === 0) return rows
  }

  throw new Error(`分页加载超过安全上限 ${maxPages} 页`)
}

function mergeUniqueRows(current, next, keyOf) {
  const rows = Array.isArray(current) ? [...current] : []
  const seen = new Set(rows.map((item, index) => String(keyOf(item, index))))
  ;(next || []).forEach((item, index) => {
    const key = String(keyOf(item, index))
    if (seen.has(key)) return
    seen.add(key)
    rows.push(item)
  })
  return rows
}

module.exports = { loadAllPages, mergeUniqueRows }
