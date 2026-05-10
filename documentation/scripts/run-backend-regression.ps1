$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8081'
$ts = Get-Date -Format 'yyyyMMddHHmmss'
$u1 = "reg1_$ts@test.local"
$u2 = "reg2_$ts@test.local"
$pwd = 'Pass12345'
$out = @()

function Add-Result {
    param(
        [string]$Test,
        [string]$Status,
        [string]$Detail
    )

    $script:out += [pscustomobject]@{
        test = $Test
        status = $Status
        detail = $Detail
    }
}

try {
    $r1 = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body (@{fullName='Reg One';email=$u1;university='CIT';major='CS';password=$pwd} | ConvertTo-Json)
    Add-Result 'AUTH_REGISTER_USER1' ($(if($r1.success){'PASS'}else{'FAIL'})) $r1.message
} catch {
    Add-Result 'AUTH_REGISTER_USER1' 'FAIL' $_.Exception.Message
}

try {
    $r2 = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json' -Body (@{email=$u1;password=$pwd} | ConvertTo-Json)
    Add-Result 'AUTH_LOGIN_USER1' ($(if($r2.success){'PASS'}else{'FAIL'})) $r2.message
} catch {
    Add-Result 'AUTH_LOGIN_USER1' 'FAIL' $_.Exception.Message
}

try {
    $r3 = Invoke-RestMethod -Method Put -Uri "$base/api/users/profile" -ContentType 'application/json' -Body (@{email=$u1;fullName='Reg One Updated';university='CIT-U';major='Data Science'} | ConvertTo-Json)
    Add-Result 'PROFILE_UPDATE' ($(if($r3.fullName -eq 'Reg One Updated'){'PASS'}else{'FAIL'})) $r3.fullName
} catch {
    Add-Result 'PROFILE_UPDATE' 'FAIL' $_.Exception.Message
}

$gid = $null
try {
    $r4 = Invoke-RestMethod -Method Post -Uri "$base/api/study-groups" -ContentType 'application/json' -Body (@{creatorEmail=$u1;subject='Regression Math';description='regression test group';day='Monday';meetingTime='10:00 AM - 11:00 AM';location='Room 101';maxMembers=4} | ConvertTo-Json)
    Add-Result 'GROUP_CREATE' ($(if($r4.success){'PASS'}else{'FAIL'})) $r4.message
} catch {
    Add-Result 'GROUP_CREATE' 'FAIL' $_.Exception.Message
}

try {
    $dash = Invoke-RestMethod -Method Get -Uri "$base/api/study-groups/dashboard?userEmail=$u1"
    $created = ($dash.myStudyGroups | Where-Object { $_.subject -eq 'Regression Math' } | Select-Object -First 1)
    $gid = $created.id
    Add-Result 'GROUP_DASHBOARD' ($(if($null -ne $gid){'PASS'}else{'FAIL'})) ("groupId=$gid")
} catch {
    Add-Result 'GROUP_DASHBOARD' 'FAIL' $_.Exception.Message
}

try {
    $reg2 = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType 'application/json' -Body (@{fullName='Reg Two';email=$u2;university='CIT';major='IT';password=$pwd} | ConvertTo-Json)
    Add-Result 'AUTH_REGISTER_USER2' ($(if($reg2.success){'PASS'}else{'FAIL'})) $reg2.message
} catch {
    Add-Result 'AUTH_REGISTER_USER2' 'FAIL' $_.Exception.Message
}

try {
    $join = Invoke-RestMethod -Method Post -Uri "$base/api/study-groups/$gid/join" -ContentType 'application/json' -Body (@{userEmail=$u2} | ConvertTo-Json)
    Add-Result 'GROUP_JOIN_USER2' ($(if($join.success){'PASS'}else{'FAIL'})) $join.message
} catch {
    Add-Result 'GROUP_JOIN_USER2' 'FAIL' $_.Exception.Message
}

try {
    $leave = Invoke-RestMethod -Method Post -Uri "$base/api/study-groups/$gid/leave" -ContentType 'application/json' -Body (@{userEmail=$u2} | ConvertTo-Json)
    Add-Result 'GROUP_LEAVE_USER2' ($(if($leave.success){'PASS'}else{'FAIL'})) $leave.message
} catch {
    Add-Result 'GROUP_LEAVE_USER2' 'FAIL' $_.Exception.Message
}

try {
    $del = Invoke-RestMethod -Method Delete -Uri "$base/api/study-groups/$gid?userEmail=$u1"
    Add-Result 'GROUP_DELETE_OWNER' ($(if($del.success){'PASS'}else{'FAIL'})) $del.message
} catch {
    Add-Result 'GROUP_DELETE_OWNER' 'FAIL' $_.Exception.Message
}

$out | ConvertTo-Json
