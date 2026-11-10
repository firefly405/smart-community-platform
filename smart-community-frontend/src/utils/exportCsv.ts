export interface CsvColumn {
  label: string
  value: string | ((row: Record<string, any>) => unknown)
}

function cell(value: unknown) {
  let text = value == null ? '' : String(value)
  if (/^[=+\-@]/.test(text)) text = `'${text}`
  return `"${text.replace(/"/g, '""')}"`
}

export function downloadCsv(filename: string, columns: CsvColumn[], rows: Record<string, any>[]) {
  const header = columns.map(column => cell(column.label)).join(',')
  const body = rows.map(row => columns.map(column => cell(
    typeof column.value === 'function' ? column.value(row) : row[column.value]
  )).join(','))
  const blob = new Blob([`\uFEFF${[header, ...body].join('\r\n')}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
