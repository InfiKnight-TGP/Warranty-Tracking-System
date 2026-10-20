Add-Type -AssemblyName System.Drawing
$iconDir = Split-Path -Parent $MyInvocation.MyCommand.Path

foreach ($size in @(16, 48, 128)) {
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics.Clear([System.Drawing.Color]::Transparent)

    $scale = $size / 48.0
    $radius = 12.0 * $scale
    $tile = New-Object System.Drawing.Drawing2D.GraphicsPath
    $tile.AddArc(0, 0, 2 * $radius, 2 * $radius, 180, 90)
    $tile.AddArc($size - 2 * $radius, 0, 2 * $radius, 2 * $radius, 270, 90)
    $tile.AddArc($size - 2 * $radius, $size - 2 * $radius, 2 * $radius, 2 * $radius, 0, 90)
    $tile.AddArc(0, $size - 2 * $radius, 2 * $radius, 2 * $radius, 90, 90)
    $tile.CloseFigure()

    $background = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(13, 17, 23))
    $wordmark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(243, 244, 246))
    $font = New-Object System.Drawing.Font('Arial', [single]($size * 0.64), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::Center
    $format.LineAlignment = [System.Drawing.StringAlignment]::Center

    $graphics.FillPath($background, $tile)
    $textBounds = New-Object System.Drawing.RectangleF(0, [single](-0.5 * $scale), $size, $size)
    $graphics.DrawString('W', $font, $wordmark, $textBounds, $format)
    $bitmap.Save((Join-Path $iconDir "icon$size.png"), [System.Drawing.Imaging.ImageFormat]::Png)

    $format.Dispose(); $font.Dispose(); $wordmark.Dispose(); $background.Dispose()
    $tile.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
}
