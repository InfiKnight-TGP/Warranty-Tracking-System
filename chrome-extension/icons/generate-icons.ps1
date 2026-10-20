Add-Type -AssemblyName System.Drawing
$iconDir = Split-Path -Parent $MyInvocation.MyCommand.Path
foreach ($size in @(16, 48, 128)) {
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $scale = $size / 48.0
    $graphics.ScaleTransform($scale, $scale)

    $tile = New-Object System.Drawing.Drawing2D.GraphicsPath
    $tile.AddArc(0, 0, 28, 28, 180, 90)
    $tile.AddArc(20, 0, 28, 28, 270, 90)
    $tile.AddArc(20, 20, 28, 28, 0, 90)
    $tile.AddArc(0, 20, 28, 28, 90, 90)
    $tile.CloseFigure()
    $tileBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(244, 247, 251))
    $graphics.FillPath($tileBrush, $tile)

    $shield = New-Object System.Drawing.Drawing2D.GraphicsPath
    $points = [System.Drawing.PointF[]]@(
        (New-Object System.Drawing.PointF(24,5)), (New-Object System.Drawing.PointF(41,11)),
        (New-Object System.Drawing.PointF(41,22)), (New-Object System.Drawing.PointF(36,34)),
        (New-Object System.Drawing.PointF(24,43)), (New-Object System.Drawing.PointF(12,34)),
        (New-Object System.Drawing.PointF(7,22)), (New-Object System.Drawing.PointF(7,11))
    )
    $shield.AddPolygon($points)
    $gradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush((New-Object System.Drawing.Point(4,4)), (New-Object System.Drawing.Point(43,44)), [System.Drawing.Color]::FromArgb(20,184,166), [System.Drawing.Color]::FromArgb(99,102,241))
    $graphics.FillPath($gradient, $shield)

    $check = New-Object System.Drawing.Pen([System.Drawing.Color]::White, 4)
    $check.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $check.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $check.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $checkPoints = [System.Drawing.PointF[]]@((New-Object System.Drawing.PointF(16,23)), (New-Object System.Drawing.PointF(21,28)), (New-Object System.Drawing.PointF(32,16)))
    $graphics.DrawLines($check, $checkPoints)

    $bitmap.Save((Join-Path $iconDir "icon$size.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $check.Dispose(); $gradient.Dispose(); $shield.Dispose(); $tileBrush.Dispose(); $tile.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
}
