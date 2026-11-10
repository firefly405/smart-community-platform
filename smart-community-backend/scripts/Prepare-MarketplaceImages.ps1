$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$assetDir = Join-Path $repo 'smart-community-backend/smart-community-admin/seed-assets/marketplace'
$runtimeDir = Join-Path $repo 'smart-community-backend/smart-community-admin/uploads/community'
$entries = @(
    @('ES202503001', 'File:Top Mark Tandem Riley, Stadskanaal (2019) 01.jpg'),
    @('ES202503002', 'File:Dyson V8 handstick vacuum.jpg'),
    @('ES202503003', 'File:A white desk is arranged in a cozy room.jpg'),
    @('ES202502001', 'File:IPad Air 4th generation - 4.jpg'),
    @('ES202502002', 'File:Stack of multicolored books on a table.jpg'),
    @('ES202502003', 'File:White desk light.jpg'),
    @('ES202501001', 'File:2022-02-17 Sony alpha 6400 IMG20220217210008.jpg'),
    @('ES202503004', 'File:Yamaha F310.jpg'),
    @('ES202503005', "File:De'Longhi espresso machines.png"),
    @('ES202503006', 'File:TwoDumbbells.JPG'),
    @('ES202501002', 'File:A treadmill.jpg'),
    @('ES202412001', 'File:Nintendo Switch – OLED-Modell (The Legend of Zelda - Tears of the Kingdom) 20230510 HOF02474 RAW-Export.png'),
    @('ES202411001', 'File:Dyson HOT + Cool AM05 026 (10769738134).jpg'),
    @('ES202412002', 'File:Baby bed 2010.jpg'),
    @('ES202410001', 'File:Casio Privia PX-310 digital piano (warm color).jpg')
)
$headers = @{ 'User-Agent' = 'SmartCommunityDemo/1.0 (local example imagery)' }
$titles = ($entries | ForEach-Object { $_[1] }) -join '|'
$uri = 'https://commons.wikimedia.org/w/api.php?action=query&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=960&format=json&titles=' + [uri]::EscapeDataString($titles)
$metadata = Invoke-RestMethod $uri -Headers $headers
$pages = @{}
foreach ($property in $metadata.query.pages.psobject.Properties) { $pages[$property.Value.title] = $property.Value }
New-Item -ItemType Directory -Path $assetDir, $runtimeDir, (Join-Path $repo 'smart-community-frontend/src/assets') -Force | Out-Null
$credits = @()
$patch = @('SET NAMES utf8mb4;', 'START TRANSACTION;')
$rollback = @('SET NAMES utf8mb4;', 'START TRANSACTION;')
$temporary = Join-Path ([System.IO.Path]::GetTempPath()) ('community-images-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $temporary | Out-Null
for ($index = 0; $index -lt $entries.Count; $index++) {
    $entry = $entries[$index]
    $info = $pages[$entry[1]].imageinfo[0]
    if (!$info -or !$info.extmetadata.LicenseShortName.value) { throw ('Missing image/license metadata: ' + $entry[1]) }
    $license = $info.extmetadata.LicenseShortName.value
    if ($license -notmatch '^(CC BY|CC0|Public domain)') { throw ('Unsupported image license: ' + $license) }
    $filename = 'd30f8a11-5b89-4f07-8d0f-' + ($index + 1).ToString('000000000000') + '.jpg'
    $destination = Join-Path $assetDir $filename
    if (!(Test-Path -LiteralPath $destination)) {
        $download = Join-Path $temporary ($index.ToString() + '.image')
        Invoke-WebRequest ($info.thumburl -replace '\?.*$', '') -Headers $headers -OutFile $download
        $original = [System.Drawing.Image]::FromFile($download)
        $bitmap = [System.Drawing.Bitmap]::new(960, 720)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::FromArgb(247, 248, 247))
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $scale = [Math]::Min(912 / $original.Width, 672 / $original.Height)
            $width = [int]($original.Width * $scale); $height = [int]($original.Height * $scale)
            $graphics.DrawImage($original, [int]((960 - $width) / 2), [int]((720 - $height) / 2), $width, $height)
            $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Jpeg)
        } finally { $graphics.Dispose(); $bitmap.Dispose(); $original.Dispose() }
        Remove-Item -LiteralPath $download
        Start-Sleep -Milliseconds 700
    }
    $runtimeImage = Join-Path $runtimeDir $filename
    if (!(Test-Path -LiteralPath $runtimeImage)) { Copy-Item -LiteralPath $destination -Destination $runtimeImage }
    $image = '/uploads/community/' + $filename
    $author = [System.Net.WebUtility]::HtmlDecode(($info.extmetadata.Artist.value -replace '<[^>]*>', '')).Trim()
    if ($entry[0] -eq 'ES202503001') { $author = 'Donald Trung Quoc Don' }
    if ($entry[0] -eq 'ES202503006') { $author = 'GeorgeStepanek' }
    $credits += [ordered]@{ itemNo = $entry[0]; image = $image; source = $info.descriptionurl; author = $author; license = $license; licenseUrl = $info.extmetadata.LicenseUrl.value }
    $patch += "UPDATE social_secondhand SET images=JSON_ARRAY('$image') WHERE item_no='$($entry[0])' AND deleted=0 AND (images IS NULL OR TRIM(images) IN ('', '[]', 'null'));"
    $rollback += "UPDATE social_secondhand SET images=NULL WHERE item_no='$($entry[0])' AND deleted=0 AND images=JSON_ARRAY('$image');"
    Write-Output ('Prepared ' + $entry[0] + ': ' + $entry[1])
}
$patch += 'COMMIT;'; $rollback += 'COMMIT;'
[System.IO.File]::WriteAllText((Join-Path $repo 'smart-community-frontend/src/assets/marketplace-image-credits.json'), ($credits | ConvertTo-Json -Depth 4), [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllLines((Join-Path $repo 'smart-community-backend/sql/patch_secondhand_images.sql'), $patch, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllLines((Join-Path $assetDir 'rollback.sql'), $rollback, [System.Text.UTF8Encoding]::new($false))
Remove-Item -LiteralPath $temporary
Write-Output 'Images prepared. Review images, then apply sql/patch_secondhand_images.sql separately.'
